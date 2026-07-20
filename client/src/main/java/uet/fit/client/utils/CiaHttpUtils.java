package uet.fit.client.utils;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.cia.communicate.CiaRequest;
import uet.fit.cia.communicate.CommitRequest;
import uet.fit.cia.communicate.CommitsResponse;
import uet.fit.cia.communicate.CompareRequest;
import uet.fit.cia.communicate.ContentResponse;
import uet.fit.cia.communicate.DifferencesResponse;
import uet.fit.cia.communicate.FileRequest;
import uet.fit.cia.communicate.FilterRequest;
import uet.fit.cia.communicate.IdentifiedRequest;
import uet.fit.cia.communicate.PathsResponse;
import uet.fit.cia.communicate.RepositoryRequest;
import uet.fit.cia.communicate.StringsResponse;
import uet.fit.cia.cpp.display.ProjectResponse;
import uet.fit.client.common.User;
import uet.fit.client.logger.LogSubscriber;
import uet.fit.client.ui.controller.cia.LogsViewController;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class CiaHttpUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(CiaHttpUtils.class);
	private static final Gson GSON = new Gson();

	private CiaHttpUtils() {
	}

	public static @NotNull CompletableFuture<StringsResponse> listRepositories(@NotNull IdentifiedRequest request) {
		return CompletableFuture.completedFuture(request)
				.thenApplyAsync(parameter -> apiCall("/cia/listRepositories", parameter, StringsResponse.class));
	}

	public static @NotNull CompletableFuture<String> fetchRepository(@NotNull RepositoryRequest request) {
		return CompletableFuture.completedFuture(request)
				.thenApplyAsync(parameter -> apiCall("/cia/fetchRepository", parameter, String.class));
	}

	public static @NotNull CompletableFuture<StringsResponse> getRefs(@NotNull RepositoryRequest request) {
		return CompletableFuture.completedFuture(request)
				.thenApplyAsync(parameter -> apiCall("/cia/getRefs", parameter, StringsResponse.class));
	}

	public static @NotNull CompletableFuture<CommitsResponse> getCommits(@NotNull FilterRequest request) {
		return CompletableFuture.completedFuture(request)
				.thenApplyAsync(parameter -> apiCall("/cia/getCommits", parameter, CommitsResponse.class));
	}

	public static @NotNull CompletableFuture<PathsResponse> findQtPros(@NotNull CommitRequest request) {
		return CompletableFuture.completedFuture(request)
				.thenApplyAsync(parameter -> apiCall("/cia/findQtPros", parameter, PathsResponse.class));
	}

	public static @NotNull CompletableFuture<ProjectResponse> compareCommits(@NotNull CiaRequest request) {
		return CompletableFuture.completedFuture(request)
				.thenApplyAsync(parameter -> apiCall("/cia/compareCommits", parameter, ProjectResponse.class));
	}

	public static @NotNull CompletableFuture<DifferencesResponse> compareTrees(@NotNull CompareRequest request) {
		return CompletableFuture.completedFuture(request)
				.thenApplyAsync(parameter -> apiCall("/cia/compareTrees", parameter, DifferencesResponse.class));
	}

	public static @NotNull CompletableFuture<ContentResponse> getFileContent(@NotNull FileRequest request) {
		return CompletableFuture.completedFuture(request)
				.thenApplyAsync(parameter -> apiCall("/cia/getFileContent", parameter, ContentResponse.class));
	}

	private static void getLogsAsync(@NotNull String identifier,
			@NotNull LogsViewController.ServerMessage serverMessage) {
		CompletableFuture.runAsync(() -> {
			LOGGER.info("Start getting server logs for " + identifier);
			for (int retries = 3; retries > 0; ) {
				try {
					Thread.sleep(1000);
				} catch (final InterruptedException exception) {
					LOGGER.error("Interrupted getting server logs for " + identifier + "!", exception);
					break;
				}
				try (final Response response = HttpUtils.AUTHORIZED_CLIENT
						.target(HttpUtils.getBaseUrl() + "/cia/getLogs")
						.request()
						.post(Entity.json(GSON.toJson(IdentifiedRequest.of(identifier))))) {
					final int status = response.getStatus();
					if (status != 200) {
						LOGGER.info("Getting server logs for " + identifier + " stopped with status " + status);
						break;
					}
					LogsViewController.updateServerMessage(serverMessage, response.readEntity(String.class));
				} catch (final Exception exception) {
					retries -= 1;
					LOGGER.error("Error getting server logs for " + identifier + "! retries = " + retries, exception);
				}
			}
		});
	}

	private static <T extends IdentifiedRequest, R> @Nullable R apiCall(@NotNull String api, @NotNull T parameter,
			@NotNull Class<R> responseClass) throws CompletionException {
		LOGGER.info("Request to " + api);
		final LogsViewController.ServerMessage serverMessage
				= LogsViewController.logApiCall("Sending request to " + api + "...");
		getLogsAsync(parameter.getIdentifier(), serverMessage);
		final String username = User.getInstance().getUsername();
		try (final LogSubscriber ignored = new LogSubscriber(username, UUID.randomUUID());
				final Response response = HttpUtils.AUTHORIZED_CLIENT
						.target(HttpUtils.getBaseUrl() + api)
						.request()
						.post(Entity.json(GSON.toJson(parameter)))) {
			if (response.getStatus() != 200) {
				LogsViewController.updateClientMessage(serverMessage,
						"Request to " + api + " returned with status " + response.getStatus(),
						LogsViewController.ServerMessage.API_FAILED);
				throw new RuntimeException("Request to " + api + " returned with status " + response.getStatus());
			}
			final String responseString = Objects.requireNonNullElse(response.readEntity(String.class), "");
			LOGGER.info("Request to " + api + " is responded with " + responseClass.getSimpleName()
					+ " (length: " + responseString.length() + ')');
			LogsViewController.updateClientMessage(serverMessage,
					"Request to " + api + " is responded with " + responseClass.getSimpleName()
							+ " (length: " + responseString.length() + ')',
					LogsViewController.ServerMessage.API_SUCCESS);
			return responseClass != String.class
					? GSON.fromJson(responseString, responseClass)
					: responseClass.cast(responseString);
		} catch (final Exception exception) {
			LOGGER.error("Error calling " + api, exception);
			throw new CompletionException("Error calling " + api, exception);
		}
	}
}
