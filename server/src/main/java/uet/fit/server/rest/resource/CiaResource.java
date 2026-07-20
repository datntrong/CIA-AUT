package uet.fit.server.rest.resource;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.cia.communicate.CiaRequest;
import uet.fit.cia.communicate.CommitRequest;
import uet.fit.cia.communicate.CompareRequest;
import uet.fit.cia.communicate.FileRequest;
import uet.fit.cia.communicate.FilterRequest;
import uet.fit.cia.communicate.IdentifiedRequest;
import uet.fit.cia.communicate.RepositoryRequest;
import uet.fit.config.Config;
import uet.fit.server.logger.cia.LogBuilder;
import uet.fit.server.rest.ServerExecutors;
import uet.fit.server.rest.service.CiaService;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

@Path("/cia")
public final class CiaResource {
	private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(CiaResource.class);
	private static final @NotNull Gson GSON = new Gson();
	private final @NotNull ExecutorService longTermExecutor = ServerExecutors.longTermExecutor();
	private final @NotNull ExecutorService shortTermExecutor = ServerExecutors.shortTermExecutor();

	private final @NotNull Map<@NotNull String, @NotNull SoftReference<@NotNull LogBuilder>> loggerMap
			= new ConcurrentHashMap<>(32, 0.5f, 16);

	private void handleError(@NotNull CompletableFuture<@NotNull Response> future, @NotNull AsyncResponse response) {
		future.exceptionally(
				throwable -> {
					LOGGER.error("throwing...", throwable);
					return Response.status(500).build();
				}
		).thenAccept(response::resume);
	}

	@GET
	@Path("/getServerLogs")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public void getServerLogs(@Suspended AsyncResponse asyncResponse) {
		handleError(
				CompletableFuture.supplyAsync(() -> {
					try {
						return Utils.zipFolder(Config.getLogPath());
					} catch (final IOException exception) {
						throw new RuntimeException(exception);
					}
				}, shortTermExecutor),
				asyncResponse
		);
	}

	@POST
	@Path("/listRepositories")
	public void listRepositories(@NotNull String body, @Suspended AsyncResponse asyncResponse) {
		handleError(
				CompletableFuture.completedFuture(body).thenApplyAsync(
						json -> createLog(
								GSON.fromJson(json, IdentifiedRequest.class),
								(builder, request) -> CiaService.listRepositories(builder)
						),
						shortTermExecutor
				),
				asyncResponse
		);
	}

	@POST
	@Path("/fetchRepository")
	public void fetchRepository(@NotNull String body, @Suspended AsyncResponse asyncResponse) {
		handleError(
				CompletableFuture.completedFuture(body).thenApplyAsync(
						json -> createLog(
								GSON.fromJson(json, RepositoryRequest.class),
								CiaService::cloneOrFetchRepository
						),
						shortTermExecutor
				),
				asyncResponse
		);
	}

	@POST
	@Path("/getRefs")
	public void getRefs(@NotNull String body, @Suspended AsyncResponse asyncResponse) {
		handleError(
				CompletableFuture.completedFuture(body).thenApplyAsync(
						json -> createLog(
								GSON.fromJson(json, RepositoryRequest.class),
								CiaService::getRefs
						),
						shortTermExecutor
				),
				asyncResponse
		);
	}

	@POST
	@Path("/getCommits")
	public void getCommits(@NotNull String body, @Suspended AsyncResponse asyncResponse) {
		handleError(
				CompletableFuture.completedFuture(body).thenApplyAsync(
						json -> createLog(
								GSON.fromJson(json, FilterRequest.class),
								CiaService::getCommits
						),
						shortTermExecutor
				),
				asyncResponse
		);
	}

	@POST
	@Path("/findQtPros")
	public void findQtPros(@NotNull String body, @Suspended AsyncResponse asyncResponse) {
		handleError(
				CompletableFuture.completedFuture(body).thenApplyAsync(
						json -> createLog(
								GSON.fromJson(json, CommitRequest.class),
								CiaService::findQtPros
						),
						shortTermExecutor
				),
				asyncResponse
		);
	}

	@POST
	@Path("/compareCommits")
	public void compareCommits(@NotNull String body, @Suspended AsyncResponse asyncResponse) {
		handleError(
				CompletableFuture.completedFuture(body).thenApplyAsync(
						json -> createLog(
								GSON.fromJson(json, CiaRequest.class),
								CiaService::compareCommits
						),
						longTermExecutor
				),
				asyncResponse
		);
	}

	@POST
	@Path("/compareTrees")
	public void compareTrees(@NotNull String body, @Suspended AsyncResponse asyncResponse) {
		handleError(
				CompletableFuture.completedFuture(body).thenApplyAsync(
						json -> createLog(
								GSON.fromJson(json, CompareRequest.class),
								CiaService::compareTrees
						),
						shortTermExecutor
				),
				asyncResponse
		);
	}

	@POST
	@Path("/getFileContent")
	public void getFileContent(@NotNull String body, @Suspended AsyncResponse asyncResponse) {
		handleError(
				CompletableFuture.completedFuture(body).thenApplyAsync(
						json -> createLog(
								GSON.fromJson(json, FileRequest.class),
								CiaService::getFileContent
						),
						shortTermExecutor
				),
				asyncResponse
		);
	}

	@POST
	@Path("/getLogs")
	public @NotNull Response getLogs(@NotNull String body) {
		try {
			final IdentifiedRequest request = GSON.fromJson(body, IdentifiedRequest.class);
			final String identifier = request.getIdentifier();
			final SoftReference<@NotNull LogBuilder> reference = loggerMap.get(identifier);
			if (reference != null) {
				final LogBuilder builder = reference.get();
				if (builder != null) {
					final String log = builder.getLog();
					if (builder.isOpen() || !log.isEmpty()) {
						return Response.ok(log, MediaType.TEXT_PLAIN).build();
					} else {
						loggerMap.remove(identifier, reference);
						return Response.status(410).build();
					}
				} else {
					loggerMap.remove(identifier, reference);
				}
			}
			return Response.status(404).build();
		} catch (final Exception exception) {
			LOGGER.error("throwing...", exception);
			return Response.status(500).build();
		}
	}

	private <Request extends IdentifiedRequest> @NotNull Response createLog(@NotNull Request request,
			@NotNull RequestHandler<Request> requestHandler) {
		final String identifier = request.getIdentifier();
		LOGGER.info("New request " + request.getClass() + " with identifier: " + identifier);
		try (final LogBuilder builder = new LogBuilder()) {
			loggerMap.put(identifier, new SoftReference<>(builder));
			final String json = GSON.toJson(requestHandler.handle(builder, request));
			return Response.ok(json, MediaType.APPLICATION_JSON).build();
		} catch (final Exception exception) {
			LOGGER.error("Throwing...", exception);
		}
		return Response.status(500).build();
	}

	private interface RequestHandler<Request extends IdentifiedRequest> {
		@NotNull Object handle(@NotNull LogBuilder builder, @NotNull Request request) throws CiaService.CiaException;
	}
}
