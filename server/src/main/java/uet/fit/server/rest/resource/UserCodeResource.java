package uet.fit.server.rest.resource;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.dto.UserDTO.FunctionDTO;
import uet.fit.dto.UserDTO.DeleteUserCodeIdDTO;
import uet.fit.dto.UserDTO.ModifyUserCodeDTO;
import uet.fit.dto.UserDTO.UserCodeDTO;
import uet.fit.dto.UserDTO.UserTypedefRow;
import uet.fit.server.rest.ServerExecutors;
import uet.fit.server.rest.service.UserCodeService;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Path("/usercode")
public class UserCodeResource implements Serializable {
	private final static Logger logger = LoggerFactory.getLogger(UserCodeResource.class);
	private final UserCodeService service = new UserCodeService();
	private final ExecutorService stes, ltes;

	public UserCodeResource() {
		stes = ServerExecutors.shortTermExecutor();
		ltes = ServerExecutors.longTermExecutor();
	}

	@POST
	@Path("/getArgument")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public void getArgument(@NotNull String body, final @Suspended AsyncResponse response) {
		stes.execute(new Runnable() {
			@Override
			public void run() {
				try {
					FunctionDTO dto = new Gson().fromJson(body, FunctionDTO.class);
					String environment = dto.getEnv();
					String uutPath = dto.getUut();
					String sutName = dto.getSutName();
					List<String> argument;
					argument = service.getFuncArgumentList(environment, sutName, uutPath);
					String json = new GsonBuilder()
							.excludeFieldsWithoutExposeAnnotation()
							.create()
							.toJson(argument.toArray(), String[].class);
					response.resume(Response.ok(json, MediaType.APPLICATION_JSON).build());
				} catch (Exception e) {
					logger.debug("Can't not get argument of this function", e);
					Response res = Response.status(520)
							.entity(e.getMessage())
							.type(MediaType.TEXT_PLAIN)
							.build();
					response.resume(res);
				}
			}
		});
	}

	@PUT
	@Path("/createUserCodeData")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public void insertUserCode(@NotNull String body, final @Suspended AsyncResponse response) {
		stes.execute(new Runnable() {
			@Override
			public void run() {
				try {
					logger.debug(body);
					UserCodeDTO userCodeDTO = new Gson().fromJson(body, UserCodeDTO.class);
					logger.debug("UserCode in UserCodeResource line 95");

						service.insertNewUserCode(userCodeDTO);

					String json = new GsonBuilder()
							.excludeFieldsWithoutExposeAnnotation()
							.create()
							.toJson(userCodeDTO, UserCodeDTO.class);
					response.resume(Response.ok(json, MediaType.APPLICATION_JSON).build());
				} catch (Exception e) {
					logger.debug("UserCode in UserCodeResource Exception", e);
					Response res = Response.status(520)
							.entity(e.getMessage())
							.type(MediaType.TEXT_PLAIN)
							.build();
					response.resume(res);
				}
			}
		});
	}

	@PUT
	@Path("/modifyUserCode")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public void modifytUserCode(@NotNull String body, final @Suspended AsyncResponse response) {
		stes.execute(new Runnable() {
			@Override
			public void run() {
				try {
					logger.debug(body);
					ModifyUserCodeDTO dto = new Gson().fromJson(body, ModifyUserCodeDTO.class);
					logger.debug("UserCode in UserCodeResource line 118");

					service.modifyUserCode(dto);

					String json = new GsonBuilder()
							.excludeFieldsWithoutExposeAnnotation()
							.create()
							.toJson(dto, ModifyUserCodeDTO.class);
					response.resume(Response.ok(json, MediaType.APPLICATION_JSON).build());
				} catch (Exception e) {
					logger.debug("Modify User Code in UserCodeResource Exception", e);
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
	@Path("/getAll")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public void getUserCode(@NotNull String body, final @Suspended AsyncResponse response) {
		stes.execute(new Runnable() {
			@Override
			public void run() {
				try {
					FunctionDTO dto = new Gson().fromJson(body, FunctionDTO.class);
					String env = dto.getEnv();
					String uut = dto.getUut();
					String sut = dto.getSutName();
					UserCodeDTO userCodeDTO = service.getlistUserCode(env, uut, sut);
					List<UserTypedefRow> list = userCodeDTO.getListContentUserCode();

					String json = new GsonBuilder()
							.create()
							.toJson(list.toArray(), UserTypedefRow[].class);
					logger.debug(json);
					response.resume(Response.ok(json, MediaType.APPLICATION_JSON).build());
				} catch (Exception e) {
					logger.debug("Can't not get usercode of this function", e);
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
	@Path("/delete")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public void deleteUserCode(@NotNull String body, final @Suspended AsyncResponse response) {
		stes.execute(new Runnable() {
			@Override
			public void run() {
				try {
					logger.debug(body);
					DeleteUserCodeIdDTO deleteUserCodeIdDTO = new Gson().fromJson(body, DeleteUserCodeIdDTO.class);
					service.deleteUserCode(deleteUserCodeIdDTO);

					String json = new GsonBuilder()
							.create()
							.toJson(deleteUserCodeIdDTO, DeleteUserCodeIdDTO.class);
					response.resume(Response.ok(json, MediaType.APPLICATION_JSON).build());
				} catch (Exception e) {
					logger.debug("UserCode in UserCodeResource Exception", e);
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
