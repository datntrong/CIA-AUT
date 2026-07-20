package uet.fit.server.rest.service;

import io.gitlab.multicia.graph.Graph;
import org.apache.commons.io.FileUtils;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.jetbrains.annotations.NotNull;
import org.mozilla.universalchardet.UniversalDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.cia.communicate.CiaRequest;
import uet.fit.cia.communicate.CommitRequest;
import uet.fit.cia.communicate.CommitResponse;
import uet.fit.cia.communicate.CommitsResponse;
import uet.fit.cia.communicate.CompareRequest;
import uet.fit.cia.communicate.ContentResponse;
import uet.fit.cia.communicate.DifferenceResponse;
import uet.fit.cia.communicate.DifferencesResponse;
import uet.fit.cia.communicate.FileRequest;
import uet.fit.cia.communicate.FilterRequest;
import uet.fit.cia.communicate.PathResponse;
import uet.fit.cia.communicate.PathsResponse;
import uet.fit.cia.communicate.RepositoryRequest;
import uet.fit.cia.communicate.StringsResponse;
import uet.fit.cia.communicate.Utils;
import uet.fit.cia.cpp.CppApi;
import uet.fit.cia.cpp.display.ProjectResponse;
import uet.fit.config.Config;
import uet.fit.server.logger.cia.LogBuilder;
import uet.fit.server.util.ServiceProgressMonitor;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CiaService {
	private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(CiaService.class);
	private static final @NotNull Path EMPTY_PATH = Path.of("");

	private CiaService() {
	}

	public static @NotNull Pair<Path, Path> initializeWorkDirs() throws CiaException {
		try {
			final Path ciaDir = Path.of(Config.getHomePath()).resolve("cia");
			final Path ciaWorkDir = ciaDir.resolve("work");
			final Path ciaSavedDir = ciaDir.resolve("save" + CppApi.VERSION);
			if (!Files.isDirectory(ciaWorkDir)) Files.createDirectories(ciaWorkDir);
			if (!Files.isDirectory(ciaSavedDir)) Files.createDirectories(ciaSavedDir);
			return Tuples.pair(ciaWorkDir, ciaSavedDir);
		} catch (final IOException e) {
			LOGGER.error("Initialize cia working directories error!", e);
			throw new CiaException("Initialize cia working directories error!", e);
		}
	}

	/**
	 * Compare two commits API
	 */
	public static @NotNull ProjectResponse compareCommits(@NotNull LogBuilder logBuilder, @NotNull CiaRequest request)
			throws CiaException {
		final Logger logger = logBuilder.wrap(LOGGER);
		final String username = request.getUsername();
		final String gitUrl = request.getGitUrl();
		final String gitUsername = request.getGitUsername();
		final String gitPassword = request.getGitPassword();
		final String commitA = request.getCommitA();
		final String commitB = request.getCommitB();
		final boolean forceReload = request.isForceReload();
		logger.info("Comparing commits " + commitA + " and " + commitB + " ...");
		final ProgressMonitor progressMonitor = new ServiceProgressMonitor(username);
		progressMonitor.beginTask("Comparing commits", ProgressMonitor.UNKNOWN);
		try {
			final GitService gitService = new GitService(logBuilder);
			final Graph graphA = getCommitGraph(logger, gitService, username, gitUrl, gitUsername, gitPassword,
					commitA, request.getProPathA(EMPTY_PATH), forceReload);
			final Graph graphB = getCommitGraph(logger, gitService, username, gitUrl, gitUsername, gitPassword,
					commitB, request.getProPathB(EMPTY_PATH), forceReload);
			logger.info("Start comparing commit " + commitA + " and commit " + commitB);
			final ProjectResponse projectResponse = CppApi.compareCppGraphForProject(graphA, graphB);
			logger.info("Success comparing commit " + commitA + " and commit " + commitB);
			return projectResponse;
		} catch (final Exception exception) {
			throw new CiaException("Failed comparing commit " + commitA + " and commit " + commitB, exception);
		} finally {
			progressMonitor.endTask();
		}
	}

	private static @NotNull Graph getCommitGraph(@NotNull Logger logger, @NotNull GitService gitService,
			@NotNull String username, @NotNull String gitUrl, @NotNull String gitUsername, @NotNull String gitPassword,
			@NotNull String commitName, @NotNull Path proFileName, boolean forceReload) throws Exception {
		logger.info("Analyzing commit: " + commitName);
		final Pair<Path, Path> workDirs = initializeWorkDirs();
		final Path ciaWorkDir = workDirs.getOne();
		final Path ciaSavedDir = workDirs.getTwo();

		final Path commitJson = ciaSavedDir.resolve(
				Base64.getUrlEncoder().encodeToString(gitUrl.getBytes(StandardCharsets.UTF_8))
						+ "." + commitName + ".json");
		/// Cache exist
		if (Files.exists(commitJson)) {
			if (forceReload) {
				logger.info("Force reloading is enabled, removing from cache: " + commitJson);
				Files.delete(commitJson);
			} else {
				logger.info("Loading from cache: " + commitJson);
				try {
					return CppApi.loadCppGraph(commitJson);
				} catch (Exception exception) {
					logger.warn("Loading from cache failed: " + commitJson, exception);
				}
				logger.info("Try reanalyzing: " + commitName);
			}
		}

		/// Not cache
		logger.info("Cloning commit: " + commitName);
		final Path commitPath = Files.createTempDirectory(ciaWorkDir, commitName + '-');
		try {
			// checkout
			final Path repositoryPath = gitService.loadRepository(gitUrl, gitUsername, gitPassword);
			gitService.checkoutCommit(username, repositoryPath, commitPath, commitName);

			// build
			logger.info("Building commit: " + commitName);
			final Graph graph = CppApi.buildQtProject(commitPath, proFileName, Path.of(Config.getGppPath()),
					Path.of(Config.getQmakePath()), Path.of(Config.getMakePath()), Config.getMakeJobsCount());

			// create cache
			logger.info("Create cache: " + commitName);
			CppApi.saveCppGraph(commitJson, graph);

			logger.info("Done Analyzing commit: " + commitName);
			return graph;
		} finally {
			// delete temp dir
			logger.info("Delete cloned directory: " + commitName);
			FileUtils.deleteQuietly(commitPath.toFile());
		}
	}

	/**
	 * Clone or fetch all from remote to current repository
	 */
	public static boolean cloneOrFetchRepository(@NotNull LogBuilder logBuilder, @NotNull RepositoryRequest request) {
		final Logger logger = logBuilder.wrap(LOGGER);
		final String username = request.getUsername();
		final String gitUrl = request.getGitUrl();
		final String gitUsername = request.getGitUsername();
		final String gitPassword = request.getGitPassword();
		logger.info("Fetching repository " + gitUrl + " ...");
		try {
			final GitService gitService = new GitService(logBuilder);
			gitService.cloneOrFetchRepository(username, gitUrl, gitUsername, gitPassword);
			logger.info("Success fetching repository " + gitUrl);
			return true;
		} catch (final Exception exception) {
			logger.error("Failed fetching repository " + gitUrl, exception);
			return false;
		}
	}

	/**
	 * Get repositories.
	 */
	public static @NotNull StringsResponse listRepositories(@NotNull LogBuilder logBuilder) throws CiaException {
		final Logger logger = logBuilder.wrap(LOGGER);
		logger.info("Listing repositories...");
		try {
			final List<URL> repositoryUrls = new GitService(logBuilder).listRepositories();
			logger.info("Success listing repositories!");
			return StringsResponse.of(repositoryUrls.stream().map(URL::toExternalForm).sorted().toArray(String[]::new));
		} catch (final Exception exception) {
			logger.error("Failed listing repositories!", exception);
			throw new CiaException("Failed listing repositories!", exception);
		}
	}

	/**
	 * Get refs
	 */
	public static @NotNull StringsResponse getRefs(@NotNull LogBuilder logBuilder, @NotNull RepositoryRequest request)
			throws CiaException {
		final Logger logger = logBuilder.wrap(LOGGER);
		final String username = request.getUsername();
		final String gitUrl = request.getGitUrl();
		final String gitUsername = request.getGitUsername();
		final String gitPassword = request.getGitPassword();
		logger.info("Getting refs from repository " + gitUrl + " ...");
		try {
			final GitService gitService = new GitService(logBuilder);
			final Path repositoryPath = gitService.loadRepository(gitUrl, gitUsername, gitPassword);
			final List<Ref> refs = gitService.getRepositoryRefs(username, repositoryPath);
			logger.info("Success getting refs from repository " + gitUrl);
			return StringsResponse.of(refs.stream().map(Ref::getName).toArray(String[]::new));
		} catch (final Exception exception) {
			logger.error("Failed getting refs from repository " + gitUrl, exception);
			throw new CiaException("Failed getting refs from repository " + gitUrl, exception);
		}
	}

	/**
	 * Get commits
	 */
	public static @NotNull CommitsResponse getCommits(@NotNull LogBuilder logBuilder, @NotNull FilterRequest request)
			throws CiaException {
		final Logger logger = logBuilder.wrap(LOGGER);
		final String username = request.getUsername();
		final String gitUrl = request.getGitUrl();
		final String gitUsername = request.getGitUsername();
		final String gitPassword = request.getGitPassword();
		final String filter = request.getFilter();
		final int startAt = Math.max(request.getStartAt(), 0);
		final int size = Math.max(request.getSize(), 1);
		logger.info("Getting commits from repository " + gitUrl + " ...");
		try {
			final GitService gitService = new GitService(logBuilder);
			final Path repositoryPath = gitService.loadRepository(gitUrl, gitUsername, gitPassword);
			final List<Map.Entry<RevCommit, List<Ref>>> commits;
			switch (request.getType()) {
				case TAG:
					commits = gitService.getCommitsWithTag(username, repositoryPath, filter, startAt, size);
					break;
				case BRANCH:
					commits = gitService.getCommitsWithBranch(username, repositoryPath, filter, startAt, size);
					break;
				case MESSAGE:
					if (!filter.isEmpty()) {
						commits = gitService.getCommitsWithMessage(username, repositoryPath, filter, startAt, size);
						break;
					}
				case COMMIT:
					if (!filter.isEmpty()) {
						commits = gitService.getCommitsWithID(username, repositoryPath, filter, startAt, size);
						break;
					}
				case ALL:
					commits = gitService.getAllCommits(username, repositoryPath, startAt, size);
					break;
				default:
					throw new AssertionError();
			}
			final List<CommitResponse> commitResponses = new ArrayList<>(commits.size());
			for (final Map.Entry<RevCommit, List<Ref>> entry : commits) {
				final RevCommit revCommit = entry.getKey();
				final List<Ref> commitRefs = entry.getValue();
				final PersonIdent author = revCommit.getAuthorIdent();
				commitResponses.add(CommitResponse.of(
						revCommit.getCommitTime(),
						revCommit.getName(),
						author.getName() + " (" + author.getEmailAddress() + ")",
						revCommit.getFullMessage(),
						commitRefs.stream().map(Ref::getName).toArray(String[]::new)
				));
			}
			logger.info("Success getting commits from repository " + gitUrl);
			return CommitsResponse.of(commitResponses.toArray(CommitResponse[]::new));
		} catch (final Exception exception) {
			logger.error("Failed getting commits from repository " + gitUrl, exception);
			throw new CiaException("Failed getting commits from repository " + gitUrl, exception);
		}
	}

	/**
	 * Find all Qt .pro files in commit
	 */
	public static @NotNull PathsResponse findQtPros(@NotNull LogBuilder logBuilder, @NotNull CommitRequest request)
			throws CiaException {
		final Logger logger = logBuilder.wrap(LOGGER);
		final String username = request.getUsername();
		final String gitUrl = request.getGitUrl();
		final String gitUsername = request.getGitUsername();
		final String gitPassword = request.getGitPassword();
		final String commit = request.getCommit();
		logger.info("Finding Qt .pro files for commit " + commit + " at " + gitUrl + " ...");
		try {
			final GitService gitService = new GitService(logBuilder);
			final Path repositoryPath = gitService.loadRepository(gitUrl, gitUsername, gitPassword);
			final Map<Path, Map.Entry<GitService.ElementType, String>> commitTree
					= gitService.getCommitTree(username, repositoryPath, commit);

			final List<PathResponse> pathResponses = new ArrayList<>();
			for (final Map.Entry<Path, Map.Entry<GitService.ElementType, String>> entry : commitTree.entrySet()) {
				final Path path = entry.getKey();
				final Map.Entry<GitService.ElementType, String> value = entry.getValue();
				if (value.getKey() == GitService.ElementType.FILE
						&& path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pro")) {
					pathResponses.add(PathResponse.of(path));
				}
			}
			logger.info("Success finding Qt .pro files for commit " + commit + " at " + gitUrl);
			return new PathsResponse(pathResponses.toArray(PathResponse[]::new));
		} catch (final Exception exception) {
			logger.error("Failed finding Qt .pro files for commit " + commit + " at " + gitUrl, exception);
			throw new CiaException("Failed finding Qt .pro files for commit " + commit + " at " + gitUrl, exception);
		}
	}

	/**
	 * Compare hashes in Git
	 */
	public static @NotNull DifferencesResponse compareTrees(@NotNull LogBuilder logBuilder,
			@NotNull CompareRequest request) throws CiaException {
		final Logger logger = logBuilder.wrap(LOGGER);
		final String username = request.getUsername();
		final String gitUrl = request.getGitUrl();
		final String gitUsername = request.getGitUsername();
		final String gitPassword = request.getGitPassword();
		final String commitA = request.getCommitA();
		final String commitB = request.getCommitB();
		logger.info("Comparing trees between commit " + commitA + " and " + commitB
				+ " for repository " + gitUrl + " ...");
		try {
			final GitService gitService = new GitService(logBuilder);
			final Path repositoryPath = gitService.loadRepository(gitUrl, gitUsername, gitPassword);
			final Map<Path, Map.Entry<GitService.ElementType, String>> treeA
					= gitService.getCommitTree(username, repositoryPath, commitA);
			final Map<Path, Map.Entry<GitService.ElementType, String>> treeB
					= gitService.getCommitTree(username, repositoryPath, commitB);

			final List<DifferenceResponse> differences = new ArrayList<>();
			for (final Map.Entry<Path, Map.Entry<GitService.ElementType, String>> entry : treeA.entrySet()) {
				final Path pathA = entry.getKey();
				final Map.Entry<GitService.ElementType, String> valueA = entry.getValue();
				final Map.Entry<GitService.ElementType, String> valueB = treeB.remove(pathA);
				final DifferenceResponse.Type typeA = DifferenceResponse.Type.valueOf(valueA.getKey().name());
				differences.add(DifferenceResponse.of(pathA, typeA,
						valueB == null ? DifferenceResponse.Change.REMOVED :
								valueA.equals(valueB)
										? DifferenceResponse.Change.UNCHANGED
										: DifferenceResponse.Change.CHANGED));
			}
			for (final Map.Entry<Path, Map.Entry<GitService.ElementType, String>> entry : treeB.entrySet()) {
				final Path pathB = entry.getKey();
				final DifferenceResponse.Type typeB = DifferenceResponse.Type.valueOf(entry.getValue().getKey().name());
				differences.add(DifferenceResponse.of(pathB, typeB, DifferenceResponse.Change.ADDED));
			}
			logger.info("Success comparing trees between commit " + commitA + " and " + commitB
					+ " for repository " + gitUrl);
			return DifferencesResponse.of(differences.toArray(DifferenceResponse[]::new));
		} catch (final Exception exception) {
			logger.error("Failed comparing trees between commit " + commitA + " and " + commitB
					+ " for repository " + gitUrl, exception);
			throw new CiaException("Failed comparing trees between commit " + commitA + " and " + commitB
					+ " for repository " + gitUrl, exception);
		}
	}

	/**
	 * Get file Content. Return null when file not found.
	 */
	public static @NotNull ContentResponse getFileContent(@NotNull LogBuilder logBuilder, @NotNull FileRequest request)
			throws CiaException {
		final Logger logger = logBuilder.wrap(LOGGER);
		final String username = request.getUsername();
		final String gitUrl = request.getGitUrl();
		final String gitUsername = request.getGitUsername();
		final String gitPassword = request.getGitPassword();
		final String commit = request.getCommit();
		final Path file = request.getPath(EMPTY_PATH);
		final String obfuscatedPath = Utils.obfuscatePath(file);
		logger.info("Getting file content for file " + obfuscatedPath + " in commit " + commit
				+ " at " + gitUrl + " ...");
		try {
			final GitService gitService = new GitService(logBuilder);
			final Path repositoryPath = gitService.loadRepository(gitUrl, gitUsername, gitPassword);
			final byte[] bytes = gitService.getFileContent(username, repositoryPath, commit, file);
			if (bytes == null) {
				logger.warn("Cannot find file " + obfuscatedPath + " in commit " + commit + " at " + gitUrl);
				return ContentResponse.of("File not found!");
			}
			logger.info("Decoding file content for file " + obfuscatedPath + " in commit " + commit + " at " + gitUrl);
			final UniversalDetector detector = new UniversalDetector();
			detector.handleData(bytes);
			detector.dataEnd();
			final String detectedCharset = detector.getDetectedCharset();
			if (detectedCharset == null || !Charset.isSupported(detectedCharset)) {
				logger.info("Success decoding file content for file " + obfuscatedPath
						+ " in commit " + commit + " at " + gitUrl + ": Not a text file!");
				return ContentResponse.of("This file is not a text file!");
			}
			logger.info("Success getting file content for file " + obfuscatedPath
					+ " in commit " + commit + " at " + gitUrl);
			return ContentResponse.of(new String(bytes, detectedCharset));
		} catch (final Exception e) {
			logger.error("Failed getting file content for file " + obfuscatedPath
					+ " in commit " + commit + " at " + gitUrl, e);
			throw new CiaException("Failed getting file content for file " + obfuscatedPath
					+ " in commit " + commit + " at " + gitUrl, e);
		}
	}

	public static final class CiaException extends Exception {
		public CiaException(@NotNull String message, @NotNull Throwable cause) {
			super(message, cause);
		}
	}
}
