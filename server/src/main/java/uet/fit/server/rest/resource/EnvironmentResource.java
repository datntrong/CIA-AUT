package uet.fit.server.rest.resource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.config.FolderConfig;
import uet.fit.aut.config.ProjectConfig;
import uet.fit.aut.env.CoverageType;
import uet.fit.aut.instrument.ProjectClone;
import uet.fit.aut.parser.obj.ProjectNode;
import uet.fit.aut.thread.task.InstrumentTask;
import uet.fit.aut.util.PathUtils;
import uet.fit.dto.AccountDTO;
import uet.fit.dto.ReportDTO;
import uet.fit.dto.env.CreateEnvironmentDTO;
import uet.fit.dto.env.DeletedEnvironmentDTO;
import uet.fit.dto.env.EnvironmentDTO;
import uet.fit.dto.env.EnvironmentListDTO;
import uet.fit.dto.env.EnvironmentNamesDTO;
import uet.fit.dto.env.InstrumentDTO;
import uet.fit.dto.logger.LogDTO;
import uet.fit.dto.report.GenerateOverviewReportDTO;
import uet.fit.server.logger.ServerLogger;
import uet.fit.server.resource_manager.CacheData;
import uet.fit.server.resource_manager.CacheManager;
import uet.fit.server.resource_manager.ProjectUsers;
import uet.fit.server.rest.ServerExecutors;
import uet.fit.server.rest.service.EnvironmentService;
import uet.fit.server.rest.service.RepoService;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.concurrent.ExecutorService;

@Path("/env")
public class EnvironmentResource {

	private static final Logger logger = LoggerFactory.getLogger(EnvironmentResource.class);

	private final EnvironmentService service;
	private final RepoService repoService;

	private final ExecutorService ltes, stes;

	public EnvironmentResource() {
		service = new EnvironmentService();
		repoService = new RepoService();

		ltes = ServerExecutors.longTermExecutor();
		stes = ServerExecutors.shortTermExecutor();
	}

	@POST
	@Path("/instrument")
	public void instrument(@NotNull String body, final @Suspended AsyncResponse response) {
		ltes.execute(new Runnable() {
			@Override
			public void run() {
				try {
					InstrumentDTO request = new GsonBuilder()
							.excludeFieldsWithoutExposeAnnotation()
							.create()
							.fromJson(body, InstrumentDTO.class);

					// check if exist environment
					String environmentName = request.getName();
					final String envPath = FolderConfig.load().getEnvironment() + File.separator + environmentName;

					String proPath = request.getProPath();
					CacheData cacheData = CacheManager.getInstance().get(proPath);
					ProjectConfig projectConfig = cacheData.getProjectConfig();
					ProjectNode root = cacheData.getProjectNode();

					// run instrument task
					ServerLogger.debug(request.getUser(), LogDTO.Position.ENVIRONMENT, "Instrumenting environment " + environmentName);
					InstrumentTask task = new InstrumentTask(envPath, projectConfig, root, request.getUser());
					task.run();

					String json = new GsonBuilder()
							.excludeFieldsWithoutExposeAnnotation()
							.create()
							.toJson(request, InstrumentDTO.class);
					response.resume(Response.ok(json, MediaType.APPLICATION_JSON).build());

				} catch (Exception e) {
					logger.error("Can't instrument environment by " + body, e);
					Response res = Response.status(520)
							.entity(e.getMessage())
							.type(MediaType.TEXT_PLAIN)
							.build();
					response.resume(res);
				}
			}
		});
	}

	@POST
	@Path("/create")
	public void create(@NotNull String body, final @Suspended AsyncResponse response) {
		ltes.execute(new Runnable() {
			@Override
			public void run() {
				try {
					CreateEnvironmentDTO request = new GsonBuilder()
							.excludeFieldsWithoutExposeAnnotation()
							.create()
							.fromJson(body, CreateEnvironmentDTO.class);

					// check if exist environment
					String environmentName = request.getName();
					if (service.isExist(environmentName))
						throw new Exception(String.format("Environment %s has already exists", environmentName));

					// check if support requested coverage
					String coverageType = request.getCoverageType();
					if (!coverageType.equals(CoverageType.STATEMENT))
						throw new Exception("Do not support coverage type " + coverageType);

					EnvironmentDTO environmentDTO = new EnvironmentDTO();

					String proFile = request.getProFile();
					environmentDTO.setProFile(proFile);
					environmentDTO.setName(environmentName);
					environmentDTO.setCoverageType(coverageType);

					String version = request.getVersion();
					environmentDTO.setCommit(version);

					String username = request.getUser();
					environmentDTO.setUser(username);

					String gitUrl = request.getGitUrl();
					environmentDTO.setGitUrl(gitUrl);

					File repoRoot = repoService.getVersionDirectory(gitUrl, version);
					environmentDTO.setProject(repoRoot.getAbsolutePath());

					// convert to absolute path
					if (!PathUtils.isAbsolute(proFile)) {
						proFile = PathUtils.absolute(proFile, repoRoot);
						environmentDTO.setProFile(proFile);
					}

					// set repo git path
					File repoGit = repoService.getCloneDirectory(username, gitUrl);
					environmentDTO.setRepository(repoGit.toString());

					// build project & cache
					final String envPath = FolderConfig.load().getEnvironment() + File.separator + environmentName;
					environmentDTO.setPath(envPath);
					service.buildAndCache(environmentDTO, true);

					// save environment in mysql db
					service.saveEnvironment(environmentDTO);

					ProjectUsers.getInstance().put(username, proFile);

					String json = new GsonBuilder()
							.excludeFieldsWithoutExposeAnnotation()
							.create()
							.toJson(environmentDTO, EnvironmentDTO.class);
					response.resume(Response.ok(json, MediaType.APPLICATION_JSON).build());

				} catch (Exception e) {
					logger.error("Can't create environment by " + body, e);
					Response res = Response.status(520)
							.entity(e.getMessage())
							.type(MediaType.TEXT_PLAIN)
							.build();
					response.resume(res);
				}
			}
		});
	}

	@GET
	@Path("/list")
	public void list(final @Suspended AsyncResponse response) {
		stes.execute(new Runnable() {
			@Override
			public void run() {
				try {
					EnvironmentListDTO listDTO = service.getAllEnvironments();
					String json = new GsonBuilder()
							.excludeFieldsWithoutExposeAnnotation()
							.create()
							.toJson(listDTO, EnvironmentListDTO.class);
					response.resume(Response.ok(json, MediaType.APPLICATION_JSON).build());
				} catch (Exception e) {
					logger.error("Can't list environments", e);
					Response res = Response.status(520)
							.entity(e.getMessage())
							.type(MediaType.TEXT_PLAIN)
							.build();
					response.resume(res);
				}
			}
		});
	}

	@POST
	@Path("/get")
	public void get(@QueryParam("user") String user, @QueryParam("name") String name,
			@NotNull String body, final @Suspended AsyncResponse response) {
		ltes.execute(new Runnable() {
			@Override
			public void run() {
				try {
					AccountDTO gitAcc = new Gson().fromJson(body, AccountDTO.class);
					String gitUser = gitAcc.getName();
					String gitPassword = gitAcc.getPassword();

					// find environment in mysql db
					EnvironmentDTO environmentDTO = service.findEnvironment(name);
					environmentDTO.setUser(user);

					String proFile = environmentDTO.getProFile();
					String gitUrl = environmentDTO.getGitUrl();

					// test clone project
					// TODO: ignore checkout
//					repoService.handleCloneRequest(user, gitUrl, gitUser, gitPassword);

					// set repo git path
					File repoGit = repoService.getCloneDirectory(user, gitUrl);
					environmentDTO.setRepository(repoGit.toString());

					// build project and cache
					service.buildAndCache(environmentDTO, false);

					ProjectUsers.getInstance().put(user, proFile);

					String json = new GsonBuilder()
							.excludeFieldsWithoutExposeAnnotation()
							.create()
							.toJson(environmentDTO, EnvironmentDTO.class);
					response.resume(Response.ok(json, MediaType.APPLICATION_JSON).build());

				} catch (Exception e) {
					logger.error("Can't open environment " + name, e);
					Response res = Response.status(520)
							.entity(e.getMessage())
							.type(MediaType.TEXT_PLAIN)
							.build();
					response.resume(res);
				}
			}
		});
	}

	@POST
	@Path("/genReport")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public void genReport(@NotNull String body, final @Suspended AsyncResponse response) {
		ltes.execute(new Runnable() {
			@Override
			public void run() {
				try {
					GenerateOverviewReportDTO generateOverviewReportDTO = new Gson().fromJson(body, GenerateOverviewReportDTO.class);
					String name = generateOverviewReportDTO.getEnv();
					String user = generateOverviewReportDTO.getUser();

					// find environment in mysql db
					EnvironmentDTO environmentDTO = null;
					environmentDTO = service.findEnvironment(name);
					environmentDTO.setUser(user);

					String proFile = environmentDTO.getProFile();
					String gitUrl = environmentDTO.getGitUrl();

					// set repo git path
					File repoGit = repoService.getCloneDirectory(user, gitUrl);
					environmentDTO.setRepository(repoGit.toString());

					// build project and cache
					service.buildAndCache(environmentDTO, false);

					ProjectUsers.getInstance().put(user, proFile);

					// gen report
					ReportDTO result = service.generateOverviewReport(environmentDTO, generateOverviewReportDTO.getUutList());

					String json = new GsonBuilder()
							.excludeFieldsWithoutExposeAnnotation()
							.create()
							.toJson(result, ReportDTO.class);

					response.resume(Response.ok(json, MediaType.APPLICATION_JSON).build());

				} catch (Exception e) {
					logger.error("Can't generate overview report for this environment", e);
					Response res = Response.status(520)
							.entity(e.getMessage())
							.type(MediaType.TEXT_PLAIN)
							.build();
					response.resume(res);
				}
			}
		});
	}

	@DELETE
	@Path("/deleted")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public void deletedEnvi(@NotNull String body, final @Suspended AsyncResponse response){
		stes.submit(new Runnable() {
			@Override
			public void run() {
				try {
					EnvironmentNamesDTO enviListDTO = new Gson().fromJson(body, EnvironmentNamesDTO.class);
					DeletedEnvironmentDTO deletedEnvironmentDTO = service.deleteEnvironmentDTO(enviListDTO);
					String json = new GsonBuilder()
							.excludeFieldsWithoutExposeAnnotation()
							.create()
							.toJson(deletedEnvironmentDTO, DeletedEnvironmentDTO.class);
					response.resume(Response.ok(json, MediaType.APPLICATION_JSON).build());
				} catch (Exception e) {
					logger.error("Can't delete environment", e);
					Response res = Response.status(520)
							.entity(e.getMessage())
							.type(MediaType.TEXT_PLAIN)
							.build();
					response.resume(res);
				}
			}
		});
	}

}
