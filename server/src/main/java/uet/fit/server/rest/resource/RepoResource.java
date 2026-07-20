package uet.fit.server.rest.resource;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.dto.repo.CheckoutRequest;
import uet.fit.dto.repo.CloneRequest;
import uet.fit.dto.repo.LocalCheckoutDTO;
import uet.fit.server.rest.ServerExecutors;
import uet.fit.server.rest.service.RepoService;

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
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Path("/repo")
public class RepoResource implements Serializable {

	private final static Logger logger = LoggerFactory.getLogger(RepoResource.class);

	private final @NotNull RepoService repoService = new RepoService();

	private final @NotNull ExecutorService stes = ServerExecutors.shortTermExecutor();
	private final @NotNull ExecutorService ltes = ServerExecutors.longTermExecutor();

	@GET
	@Path("/getAll")
	@Produces(MediaType.APPLICATION_JSON)
	@Deprecated
	public void getProFiles(@QueryParam("url") String url, @QueryParam("version") String version,
			final @Suspended AsyncResponse response) {
		stes.submit(new Runnable() {
			@Override
			public void run() {
				try {
					logger.debug("Getting .pro files");
					final File repoRoot = repoService.getVersionDirectory(url, version);
					List<String> allRepos = repoService.getAllProFiles(repoRoot);
					String json = new Gson().toJson(allRepos);
					Response res = Response.ok().entity(json).build();
					response.resume(res);
				} catch (Exception e) {
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
	@Path("/clone")
	@Produces(MediaType.APPLICATION_JSON)
	public void clone(final @Suspended AsyncResponse response, final @NotNull String body) {
		ltes.submit(new Runnable() {
			@Override
			public void run() {
				try {
					CloneRequest cloneRequest = new Gson().fromJson(body, CloneRequest.class);
					String toolUser = cloneRequest.getUser();
					String url = cloneRequest.getUrl();
					String gitUser = cloneRequest.getGitUser();
					String gitPassword = cloneRequest.getGitPassword();
					repoService.handleCloneRequest(toolUser, url, gitUser, gitPassword);
					Response res = Response.ok().build();
					response.resume(res);
				} catch (Exception e) {
					logger.error("Can't clone repository", e);
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
	@Path("/checkout")
	@Produces(MediaType.APPLICATION_JSON)
	public void checkout(final @Suspended AsyncResponse response, final @NotNull String body) {
		ltes.submit(new Runnable() {
			@Override
			public void run() {
				try {
					CheckoutRequest checkoutRequest = new Gson().fromJson(body, CheckoutRequest.class);
					String user = checkoutRequest.getUser();
					String url = checkoutRequest.getUrl();
					String version = checkoutRequest.getVersion();
					String gitUser = checkoutRequest.getGitUsername();
					String gitPassword = checkoutRequest.getGitPassword();
					List<String> proFiles = repoService.handleCheckoutRequest(user, url, version, gitUser, gitPassword);
					String json = new Gson().toJson(proFiles);
					Response res = Response.ok().entity(json).build();
					response.resume(res);
				} catch (Exception e) {
					logger.error("Can't checkout repository", e);
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
	@Path("/local-checkout")
	@Produces(MediaType.APPLICATION_JSON)
	public void checkout(final @QueryParam("user") String user, final @QueryParam("path") String path,
			final @Suspended AsyncResponse response) {
		ltes.submit(new Runnable() {
			@Override
			public void run() {
				try {
					File directory = new File(path);
					LocalCheckoutDTO dto = repoService.handleCheckoutRequest(user, directory);
					String json = new Gson().toJson(dto);
					Response res = Response.ok().entity(json).build();
					response.resume(res);
				} catch (Exception e) {
					logger.error("Can't local checkout repository", e);
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