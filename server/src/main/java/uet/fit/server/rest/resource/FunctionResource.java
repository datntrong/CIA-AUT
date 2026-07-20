package uet.fit.server.rest.resource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.dto.test.AutoGenDTO;
import uet.fit.dto.test.CreateTestDTO;
import uet.fit.dto.test.GenResultDTO;
import uet.fit.dto.test.GetTestsDTO;
import uet.fit.dto.test.TestIdsDTO;
import uet.fit.dto.test.TestListDTO;
import uet.fit.dto.test.MultiTestResultDTO;
import uet.fit.dto.test.data.TestDataDTO;
import uet.fit.server.rest.service.FunctionService;
import uet.fit.dto.func.ViewSourceDTO;
import uet.fit.server.rest.ServerExecutors;
import uet.fit.server.rest.service.TestCaseService;
import uet.fit.server.rest.service.UserCodeService;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Path("/func")
public class FunctionResource implements Serializable {

	private final static Logger logger = LoggerFactory.getLogger(FunctionResource.class);

	private final ExecutorService stes, ltes;
	private final TestCaseService testService;
	private final UserCodeService ucService;
	private final FunctionService funcService;


	public FunctionResource() {
		testService = new TestCaseService();
		funcService = new FunctionService();
		ucService = new UserCodeService();

		stes = ServerExecutors.shortTermExecutor();
		ltes = ServerExecutors.longTermExecutor();
	}

	@POST
	@Path("/newTest")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public void insertNewTestCase(@NotNull String body, final @Suspended AsyncResponse response){
		stes.submit(new Runnable() {
			@Override
			public void run() {
				try {
					CreateTestDTO createTestDTO = new Gson().fromJson(body, CreateTestDTO.class);

					//Check if test case name is existed
					String environment = createTestDTO.getEnvironment();
					System.out.println("Function resource 66:" + environment);
					String testName = createTestDTO.getName();
					String uutPath = createTestDTO.getUut();
					String sutName = createTestDTO.getSut();
					if (testService.isExistTest(environment, uutPath, sutName, testName)){
						logger.debug("Testcase's name is existed");
						response.resume(Response.status(409).entity("This testcase's name is existed").build());
					} else {
						TestDataDTO testDataDTO = testService.insertTestCase(createTestDTO);
						String json = new GsonBuilder()
								.excludeFieldsWithoutExposeAnnotation()
								.create()
								.toJson(testDataDTO, TestDataDTO.class);
						response.resume(Response.ok(json, MediaType.APPLICATION_JSON).build());
					}
				} catch (Exception e) {
					logger.error("Can't create a new test case", e);
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
	@Path("/allTests")
	@Produces(MediaType.APPLICATION_JSON)
	public void getAllTests(@NotNull String body, final @Suspended AsyncResponse response){
		stes.submit(new Runnable() {
			@Override
			public void run() {
				try {
					GetTestsDTO dto = new Gson().fromJson(body, GetTestsDTO.class);
					String profile = dto.getProPath();
					String uutPath = dto.getUut();
					String sutName = dto.getSut();
					TestListDTO testList = testService.getAllTests(profile, uutPath, sutName);
					String json = new GsonBuilder()
							.excludeFieldsWithoutExposeAnnotation()
							.create()
							.toJson(testList, TestListDTO.class);
					response.resume(Response.ok(json, MediaType.APPLICATION_JSON).build());
				} catch (Exception e) {
					logger.error("ERR: ", e);
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
	@Path("/source")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public void viewSource(@QueryParam("file") String filePath, @QueryParam("function") @Nullable String functionName,
			@QueryParam("environment") String envName, final @Suspended AsyncResponse response) {
		stes.submit(new Runnable() {
			@Override
			public void run() {
				try {
					ViewSourceDTO viewSourceDTO = funcService.viewSource(filePath, functionName, envName);
					String json = new GsonBuilder()
							.excludeFieldsWithoutExposeAnnotation()
							.create()
							.toJson(viewSourceDTO, ViewSourceDTO.class);
					response.resume(Response.ok(json, MediaType.APPLICATION_JSON).build());
				} catch (Exception e) {
					logger.error("Can't view source code", e);
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
	@Path("/gen")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public void autoGen(@NotNull String body, final @Suspended AsyncResponse response){
		ltes.submit(new Runnable() {
			@Override
			public void run() {
				try {
					AutoGenDTO dto = new Gson().fromJson(body, AutoGenDTO.class);
					// get user code from database
					HashMap<String, List<String>> userCode = ucService.getDataUserCode(dto);
					//TODO
					GenResultDTO resultDTO;

					if (dto.haveSut())
						resultDTO = testService.autoGenForSubprogram(dto, userCode);
					else
						resultDTO = testService.autoGenForUnit(dto, userCode);

					String json = new GsonBuilder()
							.excludeFieldsWithoutExposeAnnotation()
							.serializeSpecialFloatingPointValues()
							.create()
							.toJson(resultDTO, GenResultDTO.class);

					response.resume(Response.ok(json, MediaType.APPLICATION_JSON).build());
				} catch (Exception e) {
					logger.error("Can't generate test data automatically", e);
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
	@Path("/viewCoverage")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public void viewCoverage(@NotNull String body, final @Suspended AsyncResponse response) {
		ltes.submit(new Runnable() {
			@Override
			public void run() {
				try {
					TestIdsDTO testIdsDTO = new Gson().fromJson(body, TestIdsDTO.class);
					MultiTestResultDTO multiTestResultDTO = testService.viewCoverage(testIdsDTO);
					String json = new GsonBuilder()
							.excludeFieldsWithoutExposeAnnotation()
							.serializeSpecialFloatingPointValues()
							.create()
							.toJson(multiTestResultDTO, MultiTestResultDTO.class);
					response.resume(Response.ok(json, MediaType.APPLICATION_JSON).build());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
}
