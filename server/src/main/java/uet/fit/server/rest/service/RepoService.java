package uet.fit.server.rest.service;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.config.FolderConfig;
import uet.fit.aut.util.PathUtils;
import uet.fit.aut.util.QTConst;
import uet.fit.aut.util.Utils;
import uet.fit.dto.repo.LocalCheckoutDTO;
import uet.fit.server.logger.ServerLogger;
import uet.fit.server.resource_manager.EnvironmentMutex;
import uet.fit.server.rest.resource.RepoResource;
import uet.fit.server.util.RandomString;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

public class RepoService {

	private final static Logger logger = LoggerFactory.getLogger(RepoResource.class);

	public String getCommitHash(File directory) throws IOException, GitAPIException {
		File gitDirectory = directory.toPath().resolve(".git").toFile();
		final Repository repository = new FileRepository(gitDirectory);
		RevCommit latestCommit = new Git(repository).log().setMaxCount(1).call().iterator().next();
		return latestCommit.getName();
	}

	public String getRemoteUrl(File directory) throws Exception {
		File gitDirectory = directory.toPath().resolve(".git").toFile();

		if (!gitDirectory.exists() || !gitDirectory.isDirectory())
			throw new Exception(directory + " is not a valid repository");

		final Repository repository = new FileRepository(gitDirectory);
		return repository.getConfig().getString("remote", "origin", "url");
	}

	public File getCloneDirectory(String user, String urlString) throws MalformedURLException {
		String workspaceRootPath = FolderConfig.load().getWorkspace();
		File workspaceRoot = new File(workspaceRootPath);
		File cloneRepoRoot = workspaceRoot.toPath().resolve(user).resolve(FolderConfig.REPOSITORY).toFile();
		Path endPoint = getEndPoint(urlString);
		return cloneRepoRoot.toPath().resolve(endPoint).toFile();
	}

	private Path getEndPoint(String gitUrl) throws MalformedURLException {
		if (new File(gitUrl).exists()) {
			return Path.of(new File(gitUrl).getName());
		} else {
			URL url = new URL(gitUrl);
			String endPoint = url.getPath();
			if (!endPoint.endsWith(".git")) endPoint += ".git";
			endPoint = PathUtils.normalize(endPoint);
			if (endPoint.startsWith(File.separator))
				endPoint = endPoint.substring(File.separator.length());
			return Path.of(endPoint);
		}
	}

	public File getVersionDirectory(String urlString, String version) throws MalformedURLException {
		String repository = FolderConfig.load().getRepository();
		File repositoryRoot = new File(repository);
		Path endPoint = getEndPoint(urlString);
		return repositoryRoot.toPath().resolve(endPoint).resolve(version).toFile();
	}

	private File generateRandomVersionDirectory(String projectName, String randomVersion) {
		String repository = FolderConfig.load().getRepository();
		File repositoryRoot = new File(repository);
		return repositoryRoot.toPath().resolve(projectName).resolve(randomVersion).toFile();
	}

	public void checkout(@NotNull String gitUrl, @NotNull String gitUsername, @NotNull String gitPassword,
			@NotNull String username, @NotNull String commitHash, @NotNull File dest) throws Exception {
		logger.info("Start cloning commit " + commitHash + " to " + dest);
		final Path clonedProjectPath = dest.toPath();
		final GitService gitService = new GitService();
		final Path repository = gitService.loadRepository(gitUrl, gitUsername, gitPassword);
		gitService.checkoutCommit(username, repository, clonedProjectPath, commitHash);
	}

	public List<String> getAllProFiles(File root) {
		logger.debug("Getting .pro files");
		return recursivelyGetAll(root, root);
	}

	private List<String> recursivelyGetAll(final File repoRoot, File root) {
		List<String> proPaths = new ArrayList<>();
		File[] files = root.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isFile()) {
					if (file.getName().endsWith(QTConst.PRO_EXTENSION)) {
						proPaths.add(PathUtils.relative(file, repoRoot));
					}
				} else if (file.isDirectory()) {
					proPaths.addAll(recursivelyGetAll(repoRoot, file));
				}
			}
		}
		return proPaths;
	}

	public void handleCloneRequest(String toolUser, String url, String gitUser, String gitPassword) throws Exception {
		String taskId = UUID.randomUUID().toString();
		ServerLogger.progress(toolUser, taskId, "Clone project", -1, 1);
		try {
			new GitService().cloneOrFetchRepository(toolUser, url, gitUser, gitPassword);
		} finally {
			ServerLogger.progress(toolUser, taskId, "Clone project", 1, 1);
		}
	}

	public LocalCheckoutDTO handleCheckoutRequest(String user, File directory) throws Exception {
		String taskId = UUID.randomUUID().toString();
		ServerLogger.progress(user, taskId, "Checkout", -1, 1);
		File dest = null;
		try {
			String url, version;
			try {
				url = getRemoteUrl(directory);
				version = getCommitHash(directory);
				dest = getVersionDirectory(url, version);
			} catch (Exception e) {
				url = directory.getAbsolutePath();
				version = new RandomString(40).nextString();
				dest = generateRandomVersionDirectory(directory.getName(), version);
			}

			// lock to prevent another request from running
			EnvironmentMutex.getInstance().wait(dest.getAbsolutePath());
			EnvironmentMutex.getInstance().lock(dest.getAbsolutePath());

			if (!dest.exists()) {
				Utils.copy(directory, dest);
			}

			List<String> proFiles = getAllProFiles(dest);

			LocalCheckoutDTO dto = new LocalCheckoutDTO();
			dto.setProFiles(proFiles.toArray(String[]::new));
			dto.setGitUrl(url);
			dto.setCommit(version);
			return dto;

		} finally {
			ServerLogger.progress(user, taskId, "Checkout", 1, 1);

			if (dest != null)
				EnvironmentMutex.getInstance().unlock(dest.getAbsolutePath());
		}
	}

	public List<String> handleCheckoutRequest(@NotNull String user, @NotNull String url, @NotNull String version,
			@NotNull String gitUser, @NotNull String gitPassword) throws Exception {
		String taskId = UUID.randomUUID().toString();
		ServerLogger.progress(user, taskId, "Checkout", -1, 1);
		File dest = null;
		try {
			dest = getVersionDirectory(url, version);

			// lock to prevent another request from running
			EnvironmentMutex.getInstance().wait(dest.getAbsolutePath());
			EnvironmentMutex.getInstance().lock(dest.getAbsolutePath());

			if (!dest.exists()) {
				checkout(url, gitUser, gitPassword, user, version, dest);
			}

			return getAllProFiles(dest);
		} catch (Exception e) {
			if (dest != null)
				Utils.deleteFileOrFolder(dest);
			throw e;
		} finally {
			ServerLogger.progress(user, taskId, "Checkout", 1, 1);

			if (dest != null)
				EnvironmentMutex.getInstance().unlock(dest.getAbsolutePath());
		}
	}
}
