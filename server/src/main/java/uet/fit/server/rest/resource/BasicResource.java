package uet.fit.server.rest.resource;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.config.Config;
import uet.fit.server.DAO.util.JDBCConnection;
import uet.fit.server.rest.ServerExecutors;
import uet.fit.server.util.ServerConstants;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Path("/")
public class BasicResource implements Serializable {

	private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(BasicResource.class);

	private final ExecutorService es = ServerExecutors.shortTermExecutor();

	public BasicResource() {
	}

	@GET
	public Response home() throws IOException {
		String content = Utils.readResource("home.html");
		content = content.replace("{{VERSION}}", ServerConstants.TOOL_VERSION);
		return Response.ok(content, MediaType.TEXT_HTML).build();
	}

	@GET // This annotation indicates GET request
	@Path("/check")
	public void testConnection(@Suspended AsyncResponse response) {
		es.submit(() -> {
			try {
				checkFolders();
				checkMySqlSchema();
				response.resume(Response.ok("OK", MediaType.TEXT_HTML).build());
			} catch (Exception e) {
				LOGGER.error("throwing...", e);
				response.resume(Response.status(500).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build());
			}
		});
	}

	private void checkFolders() throws Exception {
		String home = Config.getHomePath();
		if (home.isBlank() || !new File(home).exists())
			throw new Exception("Invalid CIAUT Home, Please Config CIAUT Home on Server!");
	}

	private void checkMySqlSchema() throws Exception {
		try (Connection connection = JDBCConnection.getConnection();
				Statement stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery("Show tables")) {
			List<String> tables = new ArrayList<>();
			while (rs.next()) {
				tables.add(rs.getString(1));
			}

			if (tables.size() != 5)
				throw new Exception("Missing table in mysql schema");
		}
	}
}