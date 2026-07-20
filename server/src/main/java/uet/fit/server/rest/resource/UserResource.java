package uet.fit.server.rest.resource;

import com.google.gson.Gson;
import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.util.SpecialCharacter;
import uet.fit.dto.AccountDTO;
import uet.fit.server.DAO.entity.UserEntity;
import uet.fit.server.exception.MultipleOnlineUserException;
import uet.fit.server.exception.WrongAuthenticationException;
import uet.fit.server.rest.ServerExecutors;
import uet.fit.server.rest.service.UserService;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Path("/user")
public class UserResource {
	private static final Logger logger = LoggerFactory.getLogger(UserResource.class);

	private final UserService userService;

	private final ExecutorService es;

	public UserResource() {
		userService = new UserService();
		es = ServerExecutors.shortTermExecutor();
	}

	@GET
	@Path("/admin")
	@Produces(MediaType.TEXT_HTML)
	public void admin(final @Suspended AsyncResponse response) {
		es.submit(() -> {
			Response res;
			try {
				List<UserEntity> users = userService.list();
				response.resume(Response.ok(getAdminPage(users), MediaType.TEXT_HTML).build());
			} catch (Exception e) {
				logger.error("Can't access admin page", e);
				response.resume(Response.status(520)
						.entity(e.getMessage())
						.type(MediaType.TEXT_HTML)
						.build());
			}
		});
	}

	@POST
	@Path("/logon")
	public void logon(@NotNull String body, final @Suspended AsyncResponse response) {
		es.submit(() -> {
			final AccountDTO account = new Gson().fromJson(body, AccountDTO.class);
			final String username = account.getName();
			final String password = account.getPassword();
			try {
				userService.logon(username, password);
				response.resume(Response.ok("Welcome " + username, MediaType.TEXT_PLAIN)
						.build());
			} catch (WrongAuthenticationException e) {
				logger.error("Can't logon by " + username, e);
				response.resume(Response.status(401)
						.entity(e.getMessage())
						.type(MediaType.TEXT_PLAIN)
						.build());
			} catch (MultipleOnlineUserException e) {
				logger.error("Can't logon by " + username, e);
				response.resume(Response.status(202)
						.entity(e.getMessage())
						.type(MediaType.TEXT_PLAIN)
						.build());
			} catch (Exception e) {
				logger.error("Can't logon by " + username, e);
				response.resume(Response.status(520)
						.entity(e.getMessage())
						.type(MediaType.TEXT_PLAIN)
						.build());
			}
		});
	}

	@POST
	@Path("/add")
	public void add(@QueryParam("username") String username, @QueryParam("password") String password,
			final @Suspended AsyncResponse response) {
		es.submit(() -> {
			try {
				final String trimUsername = username.trim();
				final String trimPassword = password.trim();
				if (!userService.isExist(trimUsername)) {
					userService.add(trimUsername, trimPassword);
					final String content = "OK - add user " +
							trimUsername +
							" successfully";
					response.resume(Response.ok(content, MediaType.TEXT_PLAIN)
							.build());
				} else {
					final String content = "FAIL - " +
							trimUsername +
							" already exists";
					response.resume(Response.status(203)
							.entity(content)
							.type(MediaType.TEXT_PLAIN)
							.build());
				}
			} catch (Exception e) {
				response.resume(Response.status(520)
						.entity(e.getMessage())
						.type(MediaType.TEXT_PLAIN)
						.build());
			}
		});
	}

	@DELETE
	@Path("/delete")
	public void delete(@QueryParam("username") String username, final @Suspended AsyncResponse response) {
		es.submit(() -> {
			try {
				final String trimUsername = username.trim();
				userService.delete(trimUsername);
				final String content = "OK - delete user " +
						trimUsername +
						" successfully";
				response.resume(Response.ok(content, MediaType.TEXT_PLAIN)
						.build());
			} catch (Exception e) {
				response.resume(Response.status(520)
						.entity(e.getMessage())
						.type(MediaType.TEXT_PLAIN)
						.build());
			}
		});
	}

	@PUT
	@Path("/change-password")
	public void changePassword(@QueryParam("username") String username, @QueryParam("old") String oldPassword,
			@QueryParam("new") String newPassword, @QueryParam("cf") String cfPassword,
			final @Suspended AsyncResponse response) {
		es.submit(() -> {
			try {
				final String trimUsername = username.trim();
				final String trimOld = oldPassword.trim();
				final String trimNew = newPassword.trim();
				final String trimCf = cfPassword.trim();
				userService.changePassword(trimUsername, trimOld, trimNew, trimCf);
				response.resume(Response.ok("OK").build());
			} catch (Exception e) {
				response.resume(Response.status(520)
						.entity(e.getMessage())
						.type(MediaType.TEXT_PLAIN)
						.build());
			}
		});
	}

	@GET
	@Path("/logoff/{username}")
	public void logoff(@PathParam("username") String username, final @Suspended AsyncResponse response) {
		es.submit(() -> {
			try {
				userService.logoff(username);
				response.resume(Response.ok(username + " stopped working", MediaType.TEXT_PLAIN).build());
			} catch (Exception e) {
				logger.error("Can't logoff by " + username, e);
				response.resume(Response.status(520)
						.entity(e.getMessage())
						.type(MediaType.TEXT_PLAIN)
						.build());
			}
		});
	}

	private static @NotNull String getAdminPage(@NotNull List<UserEntity> users) throws IOException {
		StringBuilder userList = new StringBuilder();

		for (UserEntity user : users) {
			String name = StringEscapeUtils.escapeHtml4(user.getName());
			String status, logoff;
			if (user.isOnline()) {
				status = "online";
				String apiEndpoint = String.format("../user/logoff/%s", name);
				logoff = String.format("<a href=\"%s\">Logoff</a>", apiEndpoint);
			} else {
				status = "offline";
				logoff = SpecialCharacter.EMPTY;
			}
			status = StringEscapeUtils.escapeHtml4(status);
			userList.append("<tr><td>").append(name).append("</td><td>")
					.append(status).append("</td><td>").append(logoff).append("</td</tr>");
		}

		String html = Utils.readResource("user-admin.html");
		html = html.replace("<!--{{USER_LIST}}-->", userList);

		return html;
	}
}
