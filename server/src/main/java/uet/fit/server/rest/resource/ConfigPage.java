package uet.fit.server.rest.resource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.config.FolderConfig;
import uet.fit.config.Config;
import uet.fit.server.config.SQLConfig;
import uet.fit.server.rest.ServerExecutors;
import uet.fit.server.rest.service.CiaService;
import uet.fit.server.rest.service.UserService;
import uet.fit.server.util.ServerConstants;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Path("/config")
public final class ConfigPage {

	private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(BasicResource.class);
	private final @NotNull ExecutorService shortTermExecutor = ServerExecutors.shortTermExecutor();

	@GET
	@Produces(MediaType.TEXT_HTML)
	public @NotNull Response get() {
		try {
			return Response.ok(getContent(), MediaType.TEXT_HTML).build();
		} catch (final Exception exception) {
			LOGGER.error("throwing...", exception);
			final String message = Utils.exceptionToString(exception);
			return Response.status(500).entity(message).type(MediaType.TEXT_PLAIN).build();
		}
	}

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public void post(
			@FormParam("gppPath") String gppPath,
			@FormParam("qmakePath") String qmakePath,
			@FormParam("makePath") String makePath,
			@FormParam("homePath") String homePath,
			@FormParam("logPath") String logPath,
			@FormParam("sqlHost") String sqlHost,
			@FormParam("sqlPort") int sqlPort,
			@FormParam("sqlUser") String sqlUser,
			@FormParam("sqlPassword") String sqlPassword,
			@FormParam("funcCallAnalyze") String funcCallAnalyze,
			@FormParam("makeJobsCount") int makeJobsCount,
			@Suspended AsyncResponse response) {
		Config.setGppPath(gppPath.trim());
		Config.setQmakePath(qmakePath.trim());
		Config.setMakePath(makePath.trim());
		Config.setHomePath(homePath.trim());
		Config.setLogPath(logPath.trim());
		Config.setSqlHost(sqlHost.trim());
		Config.setSqlPort(sqlPort);
		Config.setSqlUser(sqlUser.trim());
		Config.setSqlPassword(sqlPassword.trim());
		Config.setFunctionCallAnalyze("on".equals(funcCallAnalyze));
		Config.setMakeJobsCount(makeJobsCount);
		shortTermExecutor.submit(() -> {
			try {
				CiaService.initializeWorkDirs();
				cleanFolders();
				SQLConfig.initDatabase();
				new UserService().logoffAllUsers();
				response.resume(Response.ok(getContent(), MediaType.TEXT_HTML).build());
			} catch (final Exception exception) {
				LOGGER.error("throwing...", exception);
				final String message = Utils.exceptionToString(exception);
				response.resume(Response.status(500).entity(message).type(MediaType.TEXT_PLAIN).build());
			}
		});
	}

	private static @NotNull String getContent() throws IOException {
		final String gppPath = StringEscapeUtils.escapeHtml4(Config.getGppPath());
		final String qmakePath = StringEscapeUtils.escapeHtml4(Config.getQmakePath());
		final String makePath = StringEscapeUtils.escapeHtml4(Config.getMakePath());
		final String homePath = StringEscapeUtils.escapeHtml4(Config.getHomePath());
		final String logPath = StringEscapeUtils.escapeHtml4(Config.getLogPath());
		final String sqlHost = StringEscapeUtils.escapeHtml4(Config.getSqlHost());
		final String sqlPort = String.valueOf(Config.getSqlPort());
		final String sqlUser = StringEscapeUtils.escapeHtml4(Config.getSqlUser());
		final String sqlPassword = StringEscapeUtils.escapeHtml4(Config.getSqlPassword());
		final String makeJobsCount = String.valueOf(Config.getMakeJobsCount());
		final String funcCallAnalyze = Config.isFunctionCallAnalyze() ? "checked" : "";

		return Utils.readResource("config.html")
				.replace("{{GPP_PATH}}", gppPath)
				.replace("{{QMAKE_PATH}}", qmakePath)
				.replace("{{MAKE_PATH}}", makePath)
				.replace("{{HOME_PATH}}", homePath)
				.replace("{{LOG_PATH}}", logPath)
				.replace("{{SQL_HOST}}", sqlHost)
				.replace("{{SQL_PORT}}", sqlPort)
				.replace("{{SQL_USER}}", sqlUser)
				.replace("{{SQL_PASSWORD}}", sqlPassword)
				.replace("{{FUNC_CALL}}", funcCallAnalyze)
				.replace("{{MAKE_JOBS_COUNT}}", makeJobsCount);
	}

	private void cleanFolders() {
		try {
			FolderConfig folderConfig = FolderConfig.load();

			File ws = new File(folderConfig.getWorkspace());

			if (ws.exists()) {
				File versionFile = ws.toPath().resolve("version.vs").toFile();
				if (versionFile.exists()) {
					String oldVersion = uet.fit.aut.util.Utils.readFileContent(versionFile);
					oldVersion = oldVersion.trim();
					if (!oldVersion.equals(ServerConstants.TOOL_VERSION))
						FileUtils.cleanDirectory(ws);
				}
				uet.fit.aut.util.Utils.writeContentToFile(ServerConstants.TOOL_VERSION, versionFile);
			} else
				ws.mkdirs();

			File repo = new File(folderConfig.getRepository());
			if (!repo.exists())
				repo.mkdirs();

			File env = new File(folderConfig.getEnvironment());
			if (!env.exists())
				env.mkdirs();
		} catch (IOException ignored) {
		}
	}
}
