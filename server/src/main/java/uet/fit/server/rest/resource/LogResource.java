package uet.fit.server.rest.resource;

import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.dto.logger.RapidlyEntry;
import uet.fit.server.resource_manager.LogCache;
import uet.fit.server.rest.ServerExecutors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Path("/log")
public class LogResource {
	private static final Logger logger = LoggerFactory.getLogger(LogResource.class);

	private final @NotNull ExecutorService es = ServerExecutors.shortTermExecutor();
	private final @NotNull Gson gson = new Gson();

	public LogResource() {
	}

	@GET
	public void log(@QueryParam("user") String user, final @Suspended AsyncResponse response) {
		final long endTime = System.nanoTime() + 1000000000L;
		es.execute(() -> {
			try {
//				// update last request time for user
//				CacheRequest.getInstance().put(user, System.currentTimeMillis());

				// get logs for corresponding user
				// NOTE: this implementation will wait for 1 seconds before return the log
				final LogCache instance = LogCache.getInstance();
				final List<RapidlyEntry> logs = new ArrayList<>();
				final LinkedBlockingQueue<RapidlyEntry> queue = instance.get(user);
				if (queue != null) {
					do {
						final RapidlyEntry entry = queue.poll(endTime - System.nanoTime(), TimeUnit.NANOSECONDS);
						if (entry != null) logs.add(entry);
						else break;
					} while (!queue.isEmpty());
				}

				// return response
				response.resume(Response.ok(gson.toJson(logs), MediaType.APPLICATION_JSON).build());
			} catch (Exception e) {
				logger.error("Failed to get log for user " + user, e);
				response.resume(e);
			}
		});
	}
}
