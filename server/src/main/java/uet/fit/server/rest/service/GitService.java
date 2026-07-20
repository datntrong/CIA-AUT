package uet.fit.server.rest.service;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.AndRevFilter;
import org.eclipse.jgit.revwalk.filter.MaxCountRevFilter;
import org.eclipse.jgit.revwalk.filter.MessageRevFilter;
import org.eclipse.jgit.revwalk.filter.SkipRevFilter;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.SystemReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.cia.communicate.Utils;
import uet.fit.config.Config;
import uet.fit.server.logger.cia.LogBuilder;
import uet.fit.server.util.ServiceProgressMonitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * This git service manages all repository.
 * It clones all repository as a bare repository, update them if needed, automatically clone their submodules
 * recursively and do checkout to a new directory.
 * Note that this git service is read-only.
 */
public final class GitService {
	private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(GitService.class);
	private static final @NotNull Path ROOT_PATH = Path.of("/");

	private static final long AUTHENTICATION_TIMEOUT_MS = 600000; // 10min
	private static final @NotNull Map<Map.Entry<String, String>, Map.Entry<String, Long>> AUTHENTICATION_CACHE
			= new ConcurrentHashMap<>(); // <Url, Username> -> <HashedPassword, LoggedTime>
	private static final @NotNull String AUTHENTICATION_SALT
			= Base64.getUrlEncoder().encodeToString(SecureRandom.getSeed(32));

	private static final @NotNull Map<Path, ReadWriteLock> REPOSITORY_LOCK_MAP = new ConcurrentHashMap<>();

	static {
		SystemReader.setInstance(new SystemReader() {
			@Override
			public String getHostname() {
				return "localhost";
			}

			@Override
			public String getenv(String variable) {
				return null;
			}

			@Override
			public String getProperty(String key) {
				return System.getProperty(key);
			}

			@Override
			public long getCurrentTime() {
				return System.currentTimeMillis();
			}

			@Override
			public int getTimezone(long when) {
				return TimeZone.getDefault().getOffset(when) / 60000;
			}

			@Override
			public FileBasedConfig openUserConfig(org.eclipse.jgit.lib.Config parent, FS fs) {
				return new FileBasedConfig(parent, null, fs) {
					@Override
					public void load() {
					}

					@Override
					public void save() {
					}

					@Override
					public boolean isOutdated() {
						return false;
					}
				};
			}

			@Override
			public FileBasedConfig openSystemConfig(org.eclipse.jgit.lib.Config parent, FS fs) {
				return openUserConfig(parent, fs);
			}

			@Override
			public FileBasedConfig openJGitConfig(org.eclipse.jgit.lib.Config parent, FS fs) {
				return openUserConfig(parent, fs);
			}
		});
	}

	private final @NotNull Logger logger;

	public GitService() {
		this.logger = LOGGER;
	}

	public GitService(@NotNull LogBuilder logBuilder) {
		this.logger = logBuilder.wrap(LOGGER);
	}

	private static @NotNull Path getGitDir() throws IOException {
		final Path gitDir = Path.of(Config.getHomePath()).resolve("git");
		if (!Files.isDirectory(gitDir) || !Files.isWritable(gitDir)) {
			FileUtils.deleteQuietly(gitDir.toFile());
			Files.createDirectories(gitDir);
		}
		return gitDir;
	}

	/* Return repository path */
	private static @NotNull Path getRepositoryPath(@NotNull String gitUrl) throws IOException {
		return getGitDir().resolve(Base64.getUrlEncoder().encodeToString(gitUrl.getBytes(StandardCharsets.UTF_8)));
	}

	/* Return repository path */
	private static @NotNull URL getRepositoryUrl(@NotNull Path repositoryPath) throws GitException {
		try {
			final String string = getGitDir().relativize(repositoryPath).toString();
			return new URL(new String(Base64.getUrlDecoder().decode(string), StandardCharsets.UTF_8));
		} catch (final Exception exception) {
			throw new GitException("Not a repository path!", exception);
		}
	}

	/* Return repository lock. */
	private static @NotNull ReadWriteLock getRepositoryLock(@NotNull Path repositoryPath) {
		return REPOSITORY_LOCK_MAP.computeIfAbsent(repositoryPath, path -> new ReentrantReadWriteLock(true));
	}

	/* Return repository from path. */
	private static @NotNull Repository loadRepositoryFromPath(@NotNull Path repositoryPath) throws IOException {
		return new FileRepositoryBuilder()
				.setGitDir(repositoryPath.toFile())
				.setMustExist(true)
				.build();
	}

	/* Return repository from path. */
	private static @NotNull Repository loadRepositoryFromPath(@NotNull Path repositoryPath, @NotNull Path targetPath)
			throws IOException {
		return new FileRepositoryBuilder()
				.setGitDir(repositoryPath.toFile())
				.setWorkTree(targetPath.toFile())
				.setMustExist(true)
				.build();
	}

	/* Normalize git path to relative path */
	private static @NotNull Path normalizeGitPath(@NotNull String gitPath) {
		return normalizeGitPath(Path.of(gitPath));
	}

	/* Normalize git path to relative path */
	private static @NotNull Path normalizeGitPath(@NotNull Path gitPath) {
		return gitPath.isAbsolute() ? ROOT_PATH.relativize(gitPath) : gitPath;
	}

	/*
	 * Hash the password with the given salt.
	 */
	private @NotNull String hashAuthenticationPassword(@NotNull String gitPassword, @NotNull String gitSalt) {
		final byte[] passwordBytes = gitPassword.getBytes(StandardCharsets.UTF_8);
		final byte[] saltBytes = gitSalt.getBytes(StandardCharsets.UTF_8);
		try {
			final MessageDigest instance = MessageDigest.getInstance("SHA-512");
			instance.update(saltBytes);
			instance.update(passwordBytes);
			instance.update(saltBytes);
			return Base64.getUrlEncoder().encodeToString(instance.digest());
		} catch (final NoSuchAlgorithmException exception) {
			logger.error("Cannot hash password using SHA-512!", exception);
			return Base64.getUrlEncoder().encodeToString(passwordBytes);
		}
	}

	/*
	 * Return true when it can check the authentication credential from local repository, return false otherwise.
	 */
	private boolean checkLocalAuthentication(@NotNull Repository repository, @NotNull String gitUrl,
			@NotNull String gitUsername, @NotNull String gitPassword) {
		logger.info("Checking authentication for " + gitUsername + " at " + gitUrl + " ...");
		final StoredConfig config = repository.getConfig();
		final String[] hashAndSalt = config.getStringList("cia-ut", "git-service", gitUsername);
		if (hashAndSalt != null && hashAndSalt.length == 2 && hashAndSalt[0] != null && hashAndSalt[1] != null
				&& hashAndSalt[0].equals(hashAuthenticationPassword(gitPassword, hashAndSalt[1]))) {
			logger.info("Check authentication for " + gitUsername + " at " + gitUrl + " success!");
			return true;
		} else {
			logger.info("Check authentication for " + gitUsername + " at " + gitUrl + " failed!");
			return false;
		}
	}

	/*
	 * Save the authentication credential to the local repository.
	 */
	private void saveLocalAuthentication(@NotNull Repository repository, @NotNull String gitUrl,
			@NotNull String gitUsername, @NotNull String gitPassword) throws GitException {
		logger.info("Saving authentication for " + gitUsername + " at " + gitUrl + " ...");
		final StoredConfig config = repository.getConfig();
		final String gitSalt = Base64.getUrlEncoder().encodeToString(SecureRandom.getSeed(32));
		config.setStringList("cia-ut", "git-service", gitUsername,
				List.of(hashAuthenticationPassword(gitPassword, gitSalt), gitSalt));
		try {
			config.save();
			logger.info("Success saving authentication for " + gitUsername + " at " + gitUrl);
		} catch (final Exception exception) {
			logger.error("Failed saving authentication for " + gitUsername + " at " + gitUrl, exception);
			throw new GitException("Save authentication for " + gitUsername + " at " + gitUrl + " failed!", exception);
		}
	}

	/* Check if the authentication cache map have the specified entry, and check its logged in time. */
	private boolean checkAuthenticationCache(@NotNull String gitUrl, @NotNull String gitUsername,
			@NotNull String gitPassword) {
		final Map.Entry<String, String> key = Map.entry(gitUrl, gitUsername);
		final Map.Entry<String, Long> entry = AUTHENTICATION_CACHE.get(key);
		if (entry == null) return false;
		final long loggedInTime = entry.getValue();
		final long currentTime = System.currentTimeMillis();
		if (currentTime < loggedInTime || currentTime - loggedInTime > AUTHENTICATION_TIMEOUT_MS) {
			AUTHENTICATION_CACHE.remove(key);
			return false;
		}
		return entry.getKey().equals(hashAuthenticationPassword(gitPassword, AUTHENTICATION_SALT));
	}

	/* Add new entry to the authentication cache map. */
	private void addAuthenticationCache(@NotNull String gitUrl, @NotNull String gitUsername,
			@NotNull String gitPassword) {
		AUTHENTICATION_CACHE.put(Map.entry(gitUrl, gitUsername),
				Map.entry(hashAuthenticationPassword(gitPassword, AUTHENTICATION_SALT), System.currentTimeMillis()));
	}


	/**
	 * Return true when it can read the remote repository, return false otherwise (including connection error or
	 * repository not found). This check does not use or clone the remote repository.
	 */
	public boolean checkRemoteAuthentication(@NotNull String gitUrl, @NotNull String gitUsername,
			@NotNull String gitPassword) {
		if (checkAuthenticationCache(gitUrl, gitUsername, gitPassword)) {
			logger.info("Check authentication for " + gitUsername + " at " + gitUrl + " from cache!");
			return true;
		}
		final UsernamePasswordCredentialsProvider credentialsProvider = gitUsername.isEmpty()
				? null : new UsernamePasswordCredentialsProvider(gitUsername, gitPassword);
		try {
			Git.lsRemoteRepository()
					.setRemote(gitUrl)
					.setCredentialsProvider(credentialsProvider)
					.callAsMap();
			logger.info("Check authentication for " + gitUsername + " at " + gitUrl + " success!");
			addAuthenticationCache(gitUrl, gitUsername, gitPassword);
			return true;
		} catch (final Exception exception) {
			logger.error("Check authentication for " + gitUsername + " at " + gitUrl + " failed!", exception);
			return false;
		}
	}


	/**
	 * Return a list of local repositories.
	 */
	public @NotNull List<URL> listRepositories() throws GitException {
		logger.info("Listing local repositories...");
		try (final DirectoryStream<Path> stream = Files.newDirectoryStream(getGitDir())) {
			final List<URL> repositories = new ArrayList<>();
			for (final Path repositoryPath : stream) {
				if (Files.isDirectory(repositoryPath)) {
					repositories.add(getRepositoryUrl(repositoryPath));
				}
			}
			logger.info("Success listing local repositories.");
			return repositories;
		} catch (final Exception exception) {
			logger.error("Failed listing local repositories!", exception);
			throw new GitException("Failed listing local repositories!", exception);
		}
	}


	/**
	 * Return true if load local repository and authenticate successfully.
	 */
	public @NotNull Path loadRepository(@NotNull String gitUrl, @NotNull String gitUsername,
			@NotNull String gitPassword) throws GitException {
		logger.info("Loading local repository for " + gitUsername + " at " + gitUrl + " ...");
		try {
			final Path repositoryPath = getRepositoryPath(gitUrl);
			final ReadWriteLock repositoryLock = getRepositoryLock(repositoryPath);
			final Lock readLock = repositoryLock.readLock();
			boolean isReadLocked = true;
			readLock.lock();
			try (final Repository repository = loadRepositoryFromPath(repositoryPath)) {
				if (!checkLocalAuthentication(repository, gitUrl, gitUsername, gitPassword)) {
					if (!checkRemoteAuthentication(gitUrl, gitUsername, gitPassword)) {
						throw new GitException("Git authentication failed for " + gitUsername + " at " + gitUrl);
					}
					isReadLocked = false;
					readLock.unlock();
					final Lock writeLock = repositoryLock.writeLock();
					writeLock.lock();
					try {
						saveLocalAuthentication(repository, gitUrl, gitUsername, gitPassword);
					} finally {
						writeLock.unlock();
					}
				}
				logger.info("Success loading local repository for " + gitUsername + " at " + gitUrl);
				return repositoryPath;
			} finally {
				if (isReadLocked) readLock.unlock();
			}
		} catch (final Exception exception) {
			logger.error("Failed loading local repository for " + gitUsername + " at " + gitUrl, exception);
			throw new GitException("Failed loading local repository for " + gitUsername + " at " + gitUrl, exception);
		}
	}


	/**
	 * Clone / fetch the repository, based on the newest version on the remote repository.
	 * Return the path to the repository. This path should be used as a handle instead of a normal file path.
	 */
	public void cloneOrFetchRepository(@NotNull String username, @NotNull String gitUrl,
			@NotNull String gitUsername, @NotNull String gitPassword) throws GitException {
		logger.info("Cloning or fetching repository for " + gitUsername + " at " + gitUrl + " ...");
		final ProgressMonitor progressMonitor = new ServiceProgressMonitor(username);
		final UsernamePasswordCredentialsProvider credentialsProvider = gitUsername.isEmpty()
				? null : new UsernamePasswordCredentialsProvider(gitUsername, gitPassword);
		cloneRecursively(new HashSet<>(), gitUrl, progressMonitor,
				credentialsProvider, gitUsername, gitPassword);
	}

	private void cloneRecursively(@NotNull Set<String> clonedGitUrls, @NotNull String gitUrl,
			@NotNull ProgressMonitor progressMonitor, @Nullable CredentialsProvider credentialsProvider,
			@NotNull String gitUsername, @NotNull String gitPassword) throws GitException {
		logger.info("Cloning or fetching repository at " + gitUrl + " ...");
		try {
			final Path repositoryPath = getRepositoryPath(gitUrl);
			final ReadWriteLock repositoryLock = getRepositoryLock(repositoryPath);
			final Lock readLock = repositoryLock.readLock();
			final Lock writeLock = repositoryLock.writeLock();
			boolean isWriteLocked = true;
			writeLock.lock();
			try {
				if (Files.isDirectory(repositoryPath)) {
					try (final Repository repository = FileRepositoryBuilder.create(repositoryPath.toFile())) {
						new Git(repository).fetch()
								.setProgressMonitor(progressMonitor)
								.setCredentialsProvider(credentialsProvider)
								.call();
						readLock.lock();
						writeLock.unlock();
						isWriteLocked = false;
						cloneSubmodules(clonedGitUrls, repository, progressMonitor,
								credentialsProvider, gitUsername, gitPassword);
					}
				} else {
					// maybe not a folder
					FileUtils.deleteQuietly(repositoryPath.toFile());
					// not exist, creating...
					try (final Git git = Git.cloneRepository()
							.setURI(gitUrl)
							.setMirror(true)
							.setDirectory(repositoryPath.toFile())
							.setProgressMonitor(progressMonitor)
							.setCredentialsProvider(credentialsProvider)
							.call()) {
						final Repository repository = git.getRepository();
						if (!gitUsername.isEmpty()) {
							saveLocalAuthentication(repository, gitUrl, gitUsername, gitPassword);
						}
						readLock.lock();
						writeLock.unlock();
						isWriteLocked = false;
						cloneSubmodules(clonedGitUrls, repository, progressMonitor,
								credentialsProvider, gitUsername, gitPassword);
					}
				}
				logger.info("Success cloning or fetching repository at " + gitUrl);
			} catch (final Exception exception) {
				FileUtils.deleteQuietly(repositoryPath.toFile());
				throw exception;
			} finally {
				if (isWriteLocked) {
					writeLock.unlock();
				} else {
					readLock.unlock();
				}
			}
		} catch (final Exception exception) {
			logger.info("Failed cloning or fetching repository at " + gitUrl, exception);
			throw new GitException("Failed cloning or fetching repository at " + gitUrl, exception);
		}
	}

	private void cloneSubmodules(@NotNull Set<String> clonedGitUrls, @NotNull Repository repository,
			@NotNull ProgressMonitor progressMonitor, @Nullable CredentialsProvider credentialsProvider,
			@NotNull String gitUsername, @NotNull String gitPassword) throws Exception {
		try (final RevWalk revWalk = new RevWalk(repository)) {
			revWalk.setTreeFilter(AndTreeFilter.create(
					PathFilter.create(Constants.DOT_GIT_MODULES),
					TreeFilter.ANY_DIFF
			));
			for (final Ref ref : repository.getRefDatabase().getRefs()) {
				revWalk.markStart(revWalk.parseCommit(ref.getObjectId()));
			}
			for (final RevCommit commit : revWalk) {
				try (final SubmoduleWalk walk = new SubmoduleWalk(repository)) {
					walk.setTree(commit.getTree());
					walk.setRootTree(commit.getTree());
					while (walk.next()) {
						final String url = SubmoduleWalk.getSubmoduleRemoteUrl(repository, walk.getModulesUrl());
						if (clonedGitUrls.add(url)) {
							try {
								final Path modulesPath = normalizeGitPath(walk.getModulesPath());
								logger.info("Repository have a submodule at " + Utils.obfuscatePath(modulesPath)
										+ " to repository " + url);
								new URL(url); // check for valid URL

								cloneRecursively(clonedGitUrls, url, progressMonitor,
										credentialsProvider, gitUsername, gitPassword);
							} catch (final MalformedURLException exception) {
								logger.warn(url + " is not a valid URL, skipped submodule!", exception);
							} catch (final GitException exception) {
								logger.warn("Failed cloning submodule, skipped submodule!", exception);
							}
						}
					}
				}
			}
		}
	}


	/**
	 * Checkout a commit to a new directory.
	 */
	public void checkoutCommit(@NotNull String username, @NotNull Path repositoryPath, @NotNull Path targetPath,
			@NotNull String commitHash) throws GitException {
		final URL repositoryUrl = getRepositoryUrl(repositoryPath);
		logger.info("Checking out commit " + commitHash + " to " + targetPath + " from " + repositoryUrl + " ...");
		final ProgressMonitor progressMonitor = new ServiceProgressMonitor(username);
		progressMonitor.beginTask("Checking out", ProgressMonitor.UNKNOWN);
		try {
			checkoutCommit(repositoryPath, targetPath, ObjectId.fromString(commitHash), progressMonitor);
			logger.info("Success checking out commit " + commitHash + " to " + targetPath + " from " + repositoryUrl);
		} catch (final Exception exception) {
			logger.error("Failed checking out commit " + commitHash + " to " + targetPath
					+ " from " + repositoryUrl, exception);
			throw new GitException("Failed checking out commit " + commitHash + " to " + targetPath
					+ " from " + repositoryUrl, exception);
		} finally {
			progressMonitor.endTask();
		}
	}

	private void checkoutCommit(@NotNull Path repositoryPath, @NotNull Path targetPath,
			@NotNull ObjectId commitId, @NotNull ProgressMonitor progressMonitor) throws Exception {
		final ReadWriteLock repositoryLock = getRepositoryLock(repositoryPath);
		final Lock readLock = repositoryLock.readLock();
		readLock.lock();
		try (final Repository repository = loadRepositoryFromPath(repositoryPath, targetPath);
				final ObjectReader objectReader = repository.newObjectReader();
				final RevWalk revWalk = new RevWalk(objectReader);
				final TreeWalk treeWalk = new TreeWalk(repository, objectReader)) {
			final RevTree commitTree = revWalk.parseCommit(commitId).getTree();
			treeWalk.reset(commitTree);
			treeWalk.setRecursive(true);
			while (treeWalk.next()) {
				final int fileType = treeWalk.getRawMode(0) & FileMode.TYPE_MASK;
				final String gitPath = treeWalk.getPathString();
				final Path outputPath = targetPath.resolve(normalizeGitPath(gitPath));
				switch (fileType) {
					case FileMode.TYPE_TREE:
						// directory
						Files.createDirectories(outputPath);
						break;
					case FileMode.TYPE_FILE:
					case FileMode.TYPE_SYMLINK: {
						final ObjectId objectId = treeWalk.getObjectId(0);
						final ObjectLoader loader = objectReader.open(objectId);
						Files.createDirectories(outputPath.getParent());
						if (fileType == FileMode.TYPE_FILE) {
							try (final InputStream inputStream = loader.openStream();
									final OutputStream outputStream = Files.newOutputStream(outputPath)) {
								inputStream.transferTo(outputStream);
							}
						} else {
							// NOTE: git symlink are supposed to be relative
							final String linkedTo = new String(loader.getCachedBytes(), StandardCharsets.UTF_8);
							Files.createSymbolicLink(outputPath, outputPath.resolve(linkedTo));
						}
						if (FileMode.EXECUTABLE_FILE.equals(treeWalk.getRawMode(0))) {
							// NOTE: this will throw on Windows because it doesn't support Posix File Permission
							final Set<PosixFilePermission> set = Files.getPosixFilePermissions(outputPath);
							set.add(PosixFilePermission.OWNER_EXECUTE);
							set.add(PosixFilePermission.GROUP_EXECUTE);
							set.add(PosixFilePermission.OTHERS_EXECUTE);
							Files.setPosixFilePermissions(outputPath, set);
						}
						break;
					}
					case FileMode.TYPE_GITLINK: {
						try (final SubmoduleWalk walk = SubmoduleWalk.forPath(repository, commitTree, gitPath)) {
							if (walk == null) break;
							final String url = SubmoduleWalk.getSubmoduleRemoteUrl(repository, walk.getModulesUrl());
							try {
								final Path modulesPath = normalizeGitPath(walk.getModulesPath());
								logger.info("Repository have a submodule at " + Utils.obfuscatePath(modulesPath)
										+ " to repository " + url);
								new URL(url); // check for valid URL

								final ObjectId submoduleId = walk.getObjectId();
								checkoutCommit(getRepositoryPath(url), outputPath, submoduleId, progressMonitor);
							} catch (final MalformedURLException exception) {
								logger.warn(url + " is not a valid URL, skipped submodule!", exception);
							}
						}
						break;
					}
					default:
						logger.warn("Unknown file mode: file " + gitPath + " has file mode "
								+ Integer.toHexString(fileType));
				}
				progressMonitor.update(0);
			}
			logger.info("Checking out " + commitId.name() + " to " + targetPath + " success!");
		} finally {
			readLock.unlock();
		}
	}


	/**
	 * Get file content
	 */
	public byte @Nullable [] getFileContent(@NotNull String username, @NotNull Path repositoryPath,
			@NotNull String commitHash, @NotNull Path gitFile) throws GitException {
		final String obfuscatedPath = Utils.obfuscatePath(gitFile);
		logger.info("Getting content for file " + obfuscatedPath + " from commit " + commitHash);
		final ProgressMonitor progressMonitor = new ServiceProgressMonitor(username);
		progressMonitor.beginTask("Getting file content", ProgressMonitor.UNKNOWN);
		try {
			final byte[] content = getFileContent(repositoryPath, ObjectId.fromString(commitHash),
					normalizeGitPath(gitFile), progressMonitor);
			logger.info("Success getting content for file " + obfuscatedPath + " from commit " + commitHash);
			return content;
		} catch (final Exception exception) {
			logger.error("Failed getting content for file " + obfuscatedPath
					+ " from commit " + commitHash, exception);
			throw new GitException("Failed getting content for file " + obfuscatedPath
					+ " from commit " + commitHash, exception);
		} finally {
			progressMonitor.endTask();
		}
	}

	private byte @Nullable [] getFileContent(@NotNull Path repositoryPath, @NotNull ObjectId commitId,
			@NotNull Path normalizedPath, @NotNull ProgressMonitor progressMonitor) throws Exception {
		final String obfuscatedPath = Utils.obfuscatePath(normalizedPath);
		final Lock readLock = getRepositoryLock(repositoryPath).readLock();
		readLock.lock();
		try (final Repository repository = loadRepositoryFromPath(repositoryPath);
				final ObjectReader objectReader = repository.newObjectReader();
				final RevWalk revWalk = new RevWalk(objectReader);
				final TreeWalk treeWalk = new TreeWalk(repository, objectReader)) {
			final RevTree tree = revWalk.parseCommit(commitId).getTree();
			treeWalk.reset(tree);
			treeWalk.setRecursive(false);
			while (treeWalk.next()) {
				final String gitPath = treeWalk.getPathString();
				final Path prefixPath = normalizeGitPath(gitPath);
				if (normalizedPath.startsWith(prefixPath)) {
					final int fileType = treeWalk.getRawMode(0) & FileMode.TYPE_MASK;
					switch (fileType) {
						case FileMode.TYPE_TREE:
							// directory
							treeWalk.enterSubtree();
							break;
						case FileMode.TYPE_FILE:
							if (!normalizedPath.equals(prefixPath)) {
								logger.info("File " + obfuscatedPath + " not found!");
								return null; // file not found!
							}
						case FileMode.TYPE_SYMLINK: {
							final ObjectId objectId = treeWalk.getObjectId(0);
							final ObjectLoader loader = objectReader.open(objectId);
							final byte[] bytes = loader.getCachedBytes();
							if (fileType == FileMode.TYPE_FILE) return bytes; // file found

							final String linkedTo = new String(bytes, StandardCharsets.UTF_8);
							final Path linkedToPath = prefixPath.resolve(linkedTo).normalize();
							logger.info("File " + Utils.obfuscatePath(prefixPath) + " is linked to "
									+ Utils.obfuscatePath(linkedToPath));
							return getFileContent(repositoryPath, commitId, linkedToPath, progressMonitor);
						}
						case FileMode.TYPE_GITLINK:
							try (final SubmoduleWalk walk = SubmoduleWalk.forPath(repository, tree, gitPath)) {
								if (walk == null) return null;
								final String submoduleUrl
										= SubmoduleWalk.getSubmoduleRemoteUrl(repository, walk.getModulesUrl());
								try {
									final Path modulesPath = normalizeGitPath(walk.getModulesPath());
									logger.info("Repository have a submodule at " + Utils.obfuscatePath(modulesPath)
											+ " to repository " + submoduleUrl);
									new URL(submoduleUrl); // check for valid URL

									final ObjectId submoduleId = walk.getObjectId();
									logger.info("File " + obfuscatedPath + " is in submodule " + submoduleUrl + " at "
											+ submoduleId.name());
									final Path submodulePath = getRepositoryPath(submoduleUrl);
									final Path innerFilePath = prefixPath.relativize(normalizedPath);
									return getFileContent(submodulePath, submoduleId, innerFilePath, progressMonitor);
								} catch (final MalformedURLException exception) {
									logger.warn(submoduleUrl + " is not a valid URL, skipped submodule!", exception);
									return null;
								}
							}
						default:
							logger.warn("Unknown file mode: file " + Utils.obfuscatePath(prefixPath) + " has file mode "
									+ Integer.toHexString(fileType));
					}
				}
				progressMonitor.update(0);
			}
		} finally {
			readLock.unlock();
		}
		logger.info("File " + obfuscatedPath + " not found!");
		return null;
	}


	/**
	 * Get all branches and tags from repository
	 */
	public @NotNull List<Ref> getRepositoryRefs(@NotNull String username, @NotNull Path repositoryPath)
			throws GitException {
		final URL repositoryUrl = getRepositoryUrl(repositoryPath);
		logger.info("Getting refs from " + repositoryUrl + " ...");
		final ProgressMonitor progressMonitor = new ServiceProgressMonitor(username);
		progressMonitor.beginTask("Getting refs", ProgressMonitor.UNKNOWN);
		final Lock readLock = getRepositoryLock(repositoryPath).readLock();
		readLock.lock();
		try (final Repository repository = loadRepositoryFromPath(repositoryPath)) {
			final List<Ref> refs = repository.getRefDatabase().getRefs();
			logger.info("Success getting refs from " + repositoryUrl);
			return refs;
		} catch (final Exception exception) {
			logger.error("Failed getting refs from " + repositoryUrl, exception);
			throw new GitException("Failed getting refs from " + repositoryUrl, exception);
		} finally {
			readLock.unlock();
			progressMonitor.endTask();
		}
	}


	/**
	 * Return repository commits.
	 */
	public @NotNull List<Map.Entry<RevCommit, List<Ref>>> getAllCommits(@NotNull String username,
			@NotNull Path repositoryPath, int startAt, int size) throws GitException {
		final URL repositoryUrl = getRepositoryUrl(repositoryPath);
		logger.info("Getting all commits from " + repositoryUrl + " ...");
		final ProgressMonitor progressMonitor = new ServiceProgressMonitor(username);
		progressMonitor.beginTask("Getting all commits", ProgressMonitor.UNKNOWN);
		final Lock readLock = getRepositoryLock(repositoryPath).readLock();
		readLock.lock();
		try (final Repository repository = loadRepositoryFromPath(repositoryPath)) {
			final Git git = new Git(repository);

			final List<Ref> tagRefs = git.tagList().call();
			final Map<ObjectId, List<Ref>> commitTags = getCommitTags(repository, tagRefs);

			final List<Map.Entry<RevCommit, List<Ref>>> commits = new ArrayList<>();
			for (final RevCommit commit : git.log().all().setSkip(startAt).setMaxCount(size).call()) {
				commits.add(Map.entry(commit, commitTags.getOrDefault(commit.getId(), List.of())));
			}
			logger.info("Success getting all commits from " + repositoryUrl);
			return commits;
		} catch (final Exception exception) {
			logger.error("Failed getting all commits from " + repositoryUrl, exception);
			throw new GitException("Failed getting all commits from " + repositoryUrl, exception);
		} finally {
			readLock.unlock();
			progressMonitor.endTask();
		}
	}

	/**
	 * Return repository commits with matching tags.
	 */
	public @NotNull List<Map.Entry<RevCommit, List<Ref>>> getCommitsWithTag(@NotNull String username,
			@NotNull Path repositoryPath, @NotNull String partialTag, int startAt, int size) throws GitException {
		final URL repositoryUrl = getRepositoryUrl(repositoryPath);
		logger.info("Getting tagged commits from " + repositoryUrl + " ...");
		final ProgressMonitor progressMonitor = new ServiceProgressMonitor(username);
		progressMonitor.beginTask("Getting tagged commits", ProgressMonitor.UNKNOWN);
		final Lock readLock = getRepositoryLock(repositoryPath).readLock();
		readLock.lock();
		try (final Repository repository = loadRepositoryFromPath(repositoryPath)) {
			final Git git = new Git(repository);

			final List<Ref> tagRefs = git.tagList().call().stream()
					.filter(ref -> ref.getName().replace(Constants.R_TAGS, "").contains(partialTag))
					.collect(Collectors.toList());
			final Map<ObjectId, List<Ref>> commitTags = getCommitTags(repository, tagRefs);

			final List<Map.Entry<RevCommit, List<Ref>>> commits = new ArrayList<>();
			try (final RevWalk revWalk = new RevWalk(repository)) {
				for (final Map.Entry<ObjectId, List<Ref>> entry : commitTags.entrySet()) {
					commits.add(Map.entry(revWalk.parseCommit(entry.getKey()), entry.getValue()));
				}
			}
			commits.sort(Comparator.<Map.Entry<RevCommit, List<Ref>>>comparingInt(
					entry -> entry.getKey().getCommitTime()).reversed());
			logger.info("Success getting tagged commits from " + repositoryUrl);
			final int fromIndex = Math.min(commits.size(), startAt);
			final int toIndex = Math.min(commits.size(), startAt + size);
			return commits.subList(fromIndex, toIndex);
		} catch (final Exception exception) {
			logger.error("Failed getting tagged commits from " + repositoryUrl, exception);
			throw new GitException("Failed getting tagged commits from " + repositoryUrl, exception);
		} finally {
			readLock.unlock();
			progressMonitor.endTask();
		}
	}

	/**
	 * Return repository commits in matching branches.
	 */
	public @NotNull List<Map.Entry<RevCommit, List<Ref>>> getCommitsWithBranch(@NotNull String username,
			@NotNull Path repositoryPath, @NotNull String partialBranch, int startAt, int size) throws GitException {
		final URL repositoryUrl = getRepositoryUrl(repositoryPath);
		logger.info("Getting branched commits from " + repositoryUrl + " ...");
		final ProgressMonitor progressMonitor = new ServiceProgressMonitor(username);
		progressMonitor.beginTask("Getting branched commits", ProgressMonitor.UNKNOWN);
		final Lock readLock = getRepositoryLock(repositoryPath).readLock();
		readLock.lock();
		try (final Repository repository = loadRepositoryFromPath(repositoryPath)) {
			final Git git = new Git(repository);

			final List<Ref> tagRefs = git.tagList().call();
			final Map<ObjectId, List<Ref>> commitTags = getCommitTags(repository, tagRefs);

			final LogCommand command = git.log().setSkip(startAt).setMaxCount(size);
			for (final Ref ref : git.branchList().call()) {
				if (ref.getName().replace(Constants.R_HEADS, "").contains(partialBranch)) {
					command.add(ref.getObjectId());
				}
			}

			final List<Map.Entry<RevCommit, List<Ref>>> commits = new ArrayList<>();
			for (final RevCommit commit : command.call()) {
				commits.add(Map.entry(commit, commitTags.getOrDefault(commit.getId(), List.of())));
			}
			logger.info("Success getting branched commits from " + repositoryUrl);
			return commits;
		} catch (final Exception exception) {
			logger.error("Failed getting branched commits from " + repositoryUrl, exception);
			throw new GitException("Failed getting branched commits from " + repositoryUrl, exception);
		} finally {
			readLock.unlock();
			progressMonitor.endTask();
		}
	}

	/**
	 * Return repository commits in matching messages.
	 */
	public @NotNull List<Map.Entry<RevCommit, List<Ref>>> getCommitsWithMessage(@NotNull String username,
			@NotNull Path repositoryPath, @NotNull String partialMessage, int startAt, int size) throws GitException {
		final URL repositoryUrl = getRepositoryUrl(repositoryPath);
		// todo: edit log info
		logger.info("Getting commits from " + repositoryUrl + " ...");
		final ProgressMonitor progressMonitor = new ServiceProgressMonitor(username);
		progressMonitor.beginTask("Getting commits", ProgressMonitor.UNKNOWN);
		final Lock readLock = getRepositoryLock(repositoryPath).readLock();
		readLock.lock();
		try (final Repository repository = loadRepositoryFromPath(repositoryPath)) {
			final Git git = new Git(repository);

			final List<Ref> tagRefs = git.tagList().call();
			final Map<ObjectId, List<Ref>> commitTags = getCommitTags(repository, tagRefs);

			final List<Map.Entry<RevCommit, List<Ref>>> commits = new ArrayList<>();
			for (final RevCommit commit : git.log().all().setRevFilter(AndRevFilter.create(Arrays.asList(
					MessageRevFilter.create(partialMessage),
					SkipRevFilter.create(startAt),
					MaxCountRevFilter.create(size)
			))).call()) {
				commits.add(Map.entry(commit, commitTags.getOrDefault(commit.getId(), List.of())));
			}
			logger.info("Success getting commits from " + repositoryUrl);
			return commits;
		} catch (final Exception exception) {
			logger.error("Failed getting commits from " + repositoryUrl, exception);
			throw new GitException("Failed getting commits from " + repositoryUrl, exception);
		} finally {
			readLock.unlock();
			progressMonitor.endTask();
		}
	}

	/**
	 * Return repository commits in matching ID.
	 */
	public @NotNull List<Map.Entry<RevCommit, List<Ref>>> getCommitsWithID(@NotNull String username,
			@NotNull Path repositoryPath, @NotNull String commitID, int startAt, int size) throws GitException {
		final URL repositoryUrl = getRepositoryUrl(repositoryPath);
		logger.info("Getting the commit " + commitID + " from " + repositoryUrl + " ...");
		final ProgressMonitor progressMonitor = new ServiceProgressMonitor(username);
		progressMonitor.beginTask("Getting the commit " + commitID, ProgressMonitor.UNKNOWN);
		final Lock readLock = getRepositoryLock(repositoryPath).readLock();
		readLock.lock();
		try (final Repository repository = loadRepositoryFromPath(repositoryPath)) {
			final Git git = new Git(repository);

			final List<Ref> tagRefs = git.tagList().call();
			final Map<ObjectId, List<Ref>> commitTags = getCommitTags(repository, tagRefs);

			final List<Map.Entry<RevCommit, List<Ref>>> commits = new ArrayList<>();

			if (startAt == 1) return commits; // there's at most 1 commit with a specific ID, and the commit is loaded to client

			final ObjectId objCommitID = repository.resolve(commitID);
			if (objCommitID != null) {
				final RevCommit commit = repository.parseCommit(objCommitID);
				if (commit != null) {
					commits.add(Map.entry(commit, commitTags.getOrDefault(commit.getId(), List.of())));

					logger.info("Success getting the commit " + commitID + " from " + repositoryUrl);
				} else {
					logger.info("The commit " + commitID + " not found in " + repositoryUrl);
				}
			}
			return commits;

		} catch (final Exception exception) {
			logger.error("Failed getting the commit" + commitID + " from " + repositoryUrl, exception);
			throw new GitException("Failed getting the commit" + commitID + " from " + repositoryUrl, exception);
		} finally {
			readLock.unlock();
			progressMonitor.endTask();
		}
	}

	private static @NotNull Map<ObjectId, List<Ref>> getCommitTags(@NotNull Repository repository,
			@NotNull List<Ref> tagRefs) throws IOException {
		final Map<ObjectId, List<Ref>> commitTags = new LinkedHashMap<>();
		final RefDatabase database = repository.getRefDatabase();
		for (final Ref ref : tagRefs) {
			final Ref peeledRef = ref.isPeeled() ? ref : database.peel(ref);
			final ObjectId objectId = peeledRef.getPeeledObjectId();
			commitTags.computeIfAbsent(
					objectId != null ? objectId : peeledRef.getObjectId(),
					any -> new ArrayList<>()
			).add(ref);
		}
		return commitTags;
	}


	/**
	 * Return a map contains current commit tree.
	 */
	public @NotNull Map<Path, Map.Entry<ElementType, String>> getCommitTree(@NotNull String username,
			@NotNull Path repositoryPath, @NotNull String commitHash) throws GitException {
		final URL repositoryUrl = getRepositoryUrl(repositoryPath);
		logger.info("Getting tree from commit " + commitHash + " at " + repositoryUrl + " ...");
		final ProgressMonitor progressMonitor = new ServiceProgressMonitor(username);
		progressMonitor.beginTask("Getting commit tree", ProgressMonitor.UNKNOWN);
		try {
			return getCommitTree(repositoryPath, repositoryUrl, ObjectId.fromString(commitHash), progressMonitor);
		} catch (final Exception exception) {
			logger.info("Failed getting tree from commit " + commitHash + " at " + repositoryPath, exception);
			throw new GitException("Failed getting tree from commit " + commitHash + " at " + repositoryUrl, exception);
		} finally {
			progressMonitor.endTask();
		}
	}

	private @NotNull Map<Path, Map.Entry<ElementType, String>> getCommitTree(@NotNull Path repositoryPath,
			@NotNull URL repositoryUrl, @NotNull ObjectId commitId, @NotNull ProgressMonitor progressMonitor)
			throws IOException, ConfigInvalidException {
		logger.info("Getting tree from commit " + commitId.name() + " at " + repositoryUrl + " ...");
		final Map<Path, Map.Entry<ElementType, String>> commitTree = new HashMap<>();
		final Lock readLock = getRepositoryLock(repositoryPath).readLock();
		readLock.lock();
		try (final Repository repository = loadRepositoryFromPath(repositoryPath);
				final ObjectReader objectReader = repository.newObjectReader();
				final RevWalk revWalk = new RevWalk(objectReader);
				final TreeWalk treeWalk = new TreeWalk(repository, objectReader)) {
			final RevTree tree = revWalk.parseCommit(commitId).getTree();
			treeWalk.reset(tree);
			treeWalk.setRecursive(true);
			while (treeWalk.next()) {
				final String gitPath = treeWalk.getPathString();
				final Path normalizedPath = normalizeGitPath(gitPath);
				final ObjectId objectId = treeWalk.getObjectId(0);
				final int fileType = treeWalk.getRawMode(0) & FileMode.TYPE_MASK;
				final ElementType elementType = fileType == FileMode.TYPE_TREE ? ElementType.DIRECTORY :
						fileType == FileMode.TYPE_FILE ? ElementType.FILE :
								fileType == FileMode.TYPE_SYMLINK ? ElementType.SYMLINK :
										fileType == FileMode.TYPE_GITLINK ? ElementType.SUBMODULE : null;
				if (elementType != null) {
					commitTree.put(normalizedPath, Map.entry(elementType, objectId.getName()));
					if (fileType == FileMode.TYPE_GITLINK) {
						try (final SubmoduleWalk walk = SubmoduleWalk.forPath(repository, tree, gitPath)) {
							if (walk == null) break;
							final String submoduleUrl
									= SubmoduleWalk.getSubmoduleRemoteUrl(repository, walk.getModulesUrl());
							try {
								logger.info("Repository have a submodule at " + walk.getModulesPath()
										+ " to repository " + submoduleUrl);
								new URL(submoduleUrl); // check for valid URL

								final ObjectId submoduleId = walk.getObjectId();
								logger.info("File " + normalizedPath + " is in submodule " + submoduleUrl + " at "
										+ submoduleId.name());
								final Path submodulePath = getRepositoryPath(submoduleUrl);
								final Map<Path, Map.Entry<ElementType, String>> map
										= getCommitTree(submodulePath, repositoryUrl, submoduleId, progressMonitor);
								for (final Map.Entry<Path, Map.Entry<ElementType, String>> entry : map.entrySet()) {
									final Path newPath = normalizedPath.resolve(entry.getKey());
									commitTree.put(newPath, entry.getValue());
								}
							} catch (final MalformedURLException exception) {
								logger.warn(submoduleUrl + " is not a valid URL, skipped submodule!", exception);
							}
						} catch (final TransportException | ConfigInvalidException exception) {
							logger.warn("Failed loading submodule at " + gitPath + ", skipping...", exception);
						}
					}
				} else {
					logger.warn("Unknown file mode: file " + gitPath + " has file mode "
							+ Integer.toHexString(fileType));
				}
				progressMonitor.update(0);
			}
			logger.info("Success getting tree from commit " + commitId.name() + " at " + repositoryUrl);
			return commitTree;
		} finally {
			readLock.unlock();
		}
	}

	public enum ElementType {
		FILE,
		DIRECTORY,
		SYMLINK,
		SUBMODULE
	}


	public static final class GitException extends Exception {
		public GitException(@NotNull String message) {
			super(message);
		}

		public GitException(@NotNull String message, @NotNull Throwable cause) {
			super(message, cause);
		}
	}
}
