package uet.fit.server.rest.resource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.util.Utils;
import uet.fit.dto.test.DeletedTestsDTO;
import uet.fit.dto.test.ExportListDTO;
import uet.fit.dto.test.ExportTestDTO;
import uet.fit.dto.test.ImportTestDTO;
import uet.fit.dto.test.TestCaseFileDTO;
import uet.fit.dto.test.TestDataImportDTO;
import uet.fit.dto.test.TestIdsDTO;
import uet.fit.dto.test.DuplicateTestDTO;
import uet.fit.dto.test.ModifyTestDTO;
import uet.fit.dto.test.TestListDTO;
import uet.fit.dto.test.SingleTestResultDTO;
import uet.fit.dto.test.data.TestDataDTO;
import uet.fit.server.rest.ServerExecutors;
import uet.fit.server.rest.service.TestCaseService;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.concurrent.ExecutorService;

@Path("/test")
public class TestCaseResource implements Serializable {

	private final static Logger logger = LoggerFactory.getLogger(TestCaseResource.class);

	private final TestCaseService service;
	private final ExecutorService stes, ltes;

	public TestCaseResource() {
		service = new TestCaseService();
		stes = ServerExecutors.shortTermExecutor();
		ltes = ServerExecutors.longTermExecutor();
	}

	@GET
	@Path("/getAll")
	public void getAll(@QueryParam("uut") String uut, @QueryParam("sut") String sut, @QueryParam("env") String env,
			final @Suspended AsyncResponse response) {
		stes.submit(new Runnable() {
			@Override
			public void run() {
				Response res;
				try {
					TestListDTO testListDTO = service.listTest(env, uut, sut);
					String json = new Gson().toJson(testListDTO, TestListDTO.class);
					res = Response.ok(json, MediaType.APPLICATION_JSON).build();
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					res = Response.status(520)
							.entity(e.getMessage())
							.type(MediaType.TEXT_PLAIN)
							.build();
				}
				response.resume(res);
			}
		});
	}

	@GET
	@Path("/getAllTestCase")
	public void getAllByEnv(@QueryParam("env") String env, final @Suspended AsyncResponse response) {
		stes.submit(new Runnable() {
			@Override
			public void run() {
				Response res;
				try {
					TestListDTO testListDTO = service.getAllTests(env);
					String json = new Gson().toJson(testListDTO, TestListDTO.class);
					res = Response.ok(json, MediaType.APPLICATION_JSON).build();
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					res = Response.status(520)
							.entity(e.getMessage())
							.type(MediaType.TEXT_PLAIN)
							.build();
				}
				response.resume(res);
			}
		});
	}

	@GET
	@Path("/view")
	public void get(@QueryParam("id") String id, @QueryParam("user") String user, final @Suspended AsyncResponse response) {
		stes.submit(new Runnable() {
			@Override
			public void run() {
				Response res;
				try {
					TestDataDTO testDataDTO = service.viewTest(id, user);
					String json = new GsonBuilder()
							.excludeFieldsWithoutExposeAnnotation()
							.create()
							.toJson(testDataDTO, TestDataDTO.class);
					res = Response.ok(json, MediaType.APPLICATION_JSON).build();
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					res = Response.status(520)
							.entity(e.getMessage())
							.type(MediaType.TEXT_PLAIN)
							.build();
				}
				response.resume(res);
			}
		});
	}

	@PUT
	@Path("/modify")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public void modifyTest(@NotNull String body, final @Suspended AsyncResponse response) {
		stes.execute(new Runnable() {
			@Override
			public void run() {
				try {
					ModifyTestDTO modifyDTO = new Gson().fromJson(body, ModifyTestDTO.class);
					TestDataDTO testDataDTO = service.modifyTest(modifyDTO);
					String json = new GsonBuilder()
							.excludeFieldsWithoutExposeAnnotation()
							.create()
							.toJson(testDataDTO, TestDataDTO.class);
					response.resume(Response.ok(json, MediaType.APPLICATION_JSON).build());
				} catch (Exception e) {
					logger.debug("Can't modify test case", e);
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
	public void deleteTestCases(@NotNull String body, final @Suspended AsyncResponse response) {
		stes.submit(new Runnable() {
			@Override
			public void run() {
				try {
					TestIdsDTO testListDTO = new Gson().fromJson(body, TestIdsDTO.class);
					DeletedTestsDTO deletedTestsDTO = service.deleteTestCases(testListDTO);
					String json = new GsonBuilder()
							.excludeFieldsWithoutExposeAnnotation()
							.create()
							.toJson(deletedTestsDTO, DeletedTestsDTO.class);
					response.resume(Response.ok(json, MediaType.APPLICATION_JSON).build());
				} catch (Exception e) {
					logger.error("Can't delete test case", e);
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
	@Path("/duplicate")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public void duplicateTest(@NotNull String body, final @Suspended AsyncResponse response) {
		stes.execute(new Runnable() {
			@Override
			public void run() {
				try {
					DuplicateTestDTO dto = new Gson().fromJson(body, DuplicateTestDTO.class);
					String environment = dto.getEnvironment();
					String uutPath = dto.getUut();
					String sutName = dto.getSut();
					String testName = dto.getName();

					if (service.isExistTest(environment, uutPath, sutName, testName)){
						logger.debug("Testcase's name is existed");
						response.resume(Response.status(409).entity("This testcase's name is existed").build());
					} else {
						TestDataDTO testDataDTO = service.duplicateTest(dto);
						String json = new GsonBuilder()
								.excludeFieldsWithoutExposeAnnotation()
								.create()
								.toJson(testDataDTO, TestDataDTO.class);
						response.resume(Response.ok(json, MediaType.APPLICATION_JSON).build());
					}
				} catch (Exception e) {
					logger.debug("Can't duplicate test case", e);
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
	@Path("/export")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public void exportTest(@NotNull String body, final @Suspended AsyncResponse response) {
		stes.execute(new Runnable() {
			@Override
			public void run() {
				try {
					ExportListDTO exportListDTO = new Gson().fromJson(body, ExportListDTO.class);
					service.exportTestCase(exportListDTO);
					response.resume(Response.ok("Export test cases completed!", MediaType.APPLICATION_JSON).build());
				} catch (Exception e) {
					logger.debug("Can't export test case", e);
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
	@Path("/import")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public void importTest(@NotNull String body, final @Suspended AsyncResponse response) {
		stes.execute(new Runnable() {
			@Override
			public void run() {
				try {
					ImportTestDTO importTestDTO = new Gson().fromJson(body, ImportTestDTO.class);
					String contentTestCase = Utils.readFileContent(importTestDTO.getFilePath());
					TestCaseFileDTO testCase = new Gson().fromJson(contentTestCase, TestCaseFileDTO.class);
					String environment = importTestDTO.getEnv();
					String testName = testCase.getName();
					String uut = testCase.getUut();
					String sut = testCase.getSut();
					if (service.isExistTest(environment, uut, sut, testName)){
						logger.debug("Testcase's name is existed");
						response.resume(Response.status(409).entity("This testcase's name is existed").build());
					}
					else {
						TestDataImportDTO testDataImport = service.importTestCase(importTestDTO, testCase);
						String json = new GsonBuilder()
								.excludeFieldsWithoutExposeAnnotation()
								.create()
								.toJson(testDataImport, TestDataImportDTO.class);
						response.resume(Response.ok(json, MediaType.APPLICATION_JSON).build());
					}
				} catch (Exception e) {
					logger.debug("Can't import test case", e);
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
	@Path("/run")
	public void run(@QueryParam("user") String user, @QueryParam("id") String id, final @Suspended AsyncResponse response) {
		ltes.execute(new Runnable() {
			@Override
			public void run() {
				try {
					SingleTestResultDTO dto = service.runTest(id, user);
					String json = new GsonBuilder()
							.excludeFieldsWithoutExposeAnnotation()
							.create()
							.toJson(dto);
					response.resume(Response.ok(json, MediaType.APPLICATION_JSON).build());
				} catch (Exception e) {
					logger.error("Can't execute " + id, e);
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
