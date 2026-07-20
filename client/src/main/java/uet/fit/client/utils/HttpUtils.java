package uet.fit.client.utils;

import com.google.gson.GsonBuilder;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.client.common.ClientFilterRequest;
import uet.fit.client.common.User;
import uet.fit.client.logger.WaitingLogs;
import uet.fit.client.thread.callback.OnReceiveLog;
import uet.fit.dto.AccountDTO;
import uet.fit.dto.UserDTO.FunctionDTO;
import uet.fit.dto.UserDTO.DeleteUserCodeIdDTO;
import uet.fit.dto.UserDTO.ModifyUserCodeDTO;
import uet.fit.dto.UserDTO.UserCodeDTO;
import uet.fit.dto.UserDTO.UserTypedefRow;
import uet.fit.dto.deserializer.LogImporter;
import uet.fit.dto.env.CreateEnvironmentDTO;
import uet.fit.dto.env.EnvironmentNamesDTO;
import uet.fit.dto.env.InstrumentDTO;
import uet.fit.dto.env.Source;
import uet.fit.dto.logger.RapidlyEntry;
import uet.fit.dto.repo.CheckoutRequest;
import uet.fit.dto.repo.CloneRequest;
import uet.fit.dto.report.GenerateOverviewReportDTO;
import uet.fit.dto.test.AutoGenDTO;
import uet.fit.dto.test.CreateTestDTO;
import uet.fit.dto.test.DuplicateTestDTO;
import uet.fit.dto.test.ExportListDTO;
import uet.fit.dto.test.ExportTestDTO;
import uet.fit.dto.test.ImportTestDTO;
import uet.fit.dto.test.ModifyTestDTO;
import uet.fit.dto.test.TestIdsDTO;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

public class HttpUtils {

	private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);

	private static @Nullable String BASE_URL;

	public static @NotNull String getBaseUrl() {
		return Objects.requireNonNull(BASE_URL);
	}

	public static void setBaseUrl(@NotNull String baseUrl) {
		BASE_URL = baseUrl;
	}

	public static boolean isBaseUrlLocalhost() {
		try {
			final String host = new URL(getBaseUrl()).getHost();
			return "localhost".equals(host) || "127.0.0.1".equals(host);
		} catch (final IOException ignored) {
		}
		return false;
	}

	public static final Client AUTHORIZED_CLIENT = ClientBuilder.newBuilder().build()
			.register(ClientFilterRequest.class);
	public static final Client UNAUTHORIZED_CLIENT = ClientBuilder.newBuilder().build();

	public static boolean testConnection(@NotNull String baseUrl) {
		try {
			final int status = UNAUTHORIZED_CLIENT
					.target(baseUrl + "/check")
					.request()
					.get()
					.getStatus();
			return status == 200;
		} catch (final ProcessingException e) {
			return false;
		}
	}

	public static Response createNewEnv(String name, String owner, String coverageType,
			String proPath, String gitUrl, String version) {
		CreateEnvironmentDTO environmentDTO = new CreateEnvironmentDTO();
		environmentDTO.setName(name);
		environmentDTO.setUser(owner);
		environmentDTO.setCoverageType(coverageType);
		environmentDTO.setProFile(proPath);
		environmentDTO.setGitUrl(gitUrl);
		environmentDTO.setVersion(version);

		String json = new GsonBuilder()
				.excludeFieldsWithoutExposeAnnotation()
				.create()
				.toJson(environmentDTO);

		return AUTHORIZED_CLIENT
				.target(getBaseUrl() + "/env/create")
				.request()
				.post(Entity.json(json));
	}

	public static Response instrument(String name, String owner, String proPath) {
		InstrumentDTO dto = new InstrumentDTO();
		dto.setName(name);
		dto.setUser(owner);
		dto.setProPath(proPath);

		String json = new GsonBuilder()
				.excludeFieldsWithoutExposeAnnotation()
				.create()
				.toJson(dto);

		return AUTHORIZED_CLIENT
				.target(getBaseUrl() + "/env/instrument")
				.request()
				.post(Entity.json(json));
	}

	public static void subscribeLog(@NotNull String username, @NotNull OnReceiveLog onReceiveLog) {
		final Thread thread = new Thread(() -> {
			while (!WaitingLogs.getInstance().isEmpty()) {
				try {
					final Response response = AUTHORIZED_CLIENT
							.target(getBaseUrl() + "/log?user=" + username)
							.request()
							.get();
					final String entity = response.readEntity(String.class);
					final List<RapidlyEntry> logs = LogImporter.fromJson(entity);
					Platform.runLater(() -> onReceiveLog.onSucceeded(logs));
				} catch (final Exception e) {
					e.printStackTrace();
					Platform.runLater(() -> onReceiveLog.onFailed(e));
				}
			}
		});
		thread.setDaemon(true);
		thread.start();
	}

	public static Response getEnv(String user, String name, String gitUser, String gitPassword) {
		user = URLEncoder.encode(user, StandardCharsets.UTF_8);
		name = URLEncoder.encode(name, StandardCharsets.UTF_8);
		gitUser = URLEncoder.encode(gitUser, StandardCharsets.UTF_8);
		gitPassword = URLEncoder.encode(gitPassword, StandardCharsets.UTF_8);

		AccountDTO gitAcc = new AccountDTO(gitUser, gitPassword);

		return AUTHORIZED_CLIENT
				.target(getBaseUrl() + "/env/get?name=" + name + "&user=" + user
//						+ "&gitUser=" + gitUser + "&gitPassword=" + gitPassword
				)
				.request()
				.post(Entity.json(gitAcc));
	}

	public static Response deleteEnv(String user, List<String> eviNames) {
//		user = URLEncoder.encode(user, StandardCharsets.UTF_8);
		EnvironmentNamesDTO environmentNamesDTO = new EnvironmentNamesDTO();
		environmentNamesDTO.setNames(eviNames);
		environmentNamesDTO.setUser(user);
		return AUTHORIZED_CLIENT
				.target(getBaseUrl() + "/env/deleted")
				.request()
				.method("DELETE", Entity.json(environmentNamesDTO));
	}


	public static Response getAllRepos(String url, String version) {
		url = URLEncoder.encode(url, StandardCharsets.UTF_8);
		version = URLEncoder.encode(version, StandardCharsets.UTF_8);

		return AUTHORIZED_CLIENT
				.target(getBaseUrl() + "/repo/getAll?url=" + url + "&version=" + version)
				.request()
				.get();
	}

	public static Response cloneRepository(String user, String url, String gitUser, String gitPassword) {
		CloneRequest cloneRequest = new CloneRequest();
		cloneRequest.setUser(user);
		cloneRequest.setUrl(url);
		cloneRequest.setGitUser(gitUser);
		cloneRequest.setGitPassword(gitPassword);

		return AUTHORIZED_CLIENT
				.target(getBaseUrl() + "/repo/clone")
				.request()
				.post(Entity.json(cloneRequest));
	}

	public static Response checkout(String user, String url, String version, String gitUser, String gitPassword) {
		CheckoutRequest checkoutRequest = new CheckoutRequest();
		checkoutRequest.setUser(user);
		checkoutRequest.setUrl(url);
		checkoutRequest.setVersion(version);
		checkoutRequest.setGitUsername(gitUser);
		checkoutRequest.setGitPassword(gitPassword);

		return AUTHORIZED_CLIENT
				.target(getBaseUrl() + "/repo/checkout")
				.request()
				.post(Entity.json(checkoutRequest));
	}

	public static Response localCheckout(String user, String projectPath) {
		user = URLEncoder.encode(user, StandardCharsets.UTF_8);
		projectPath = URLEncoder.encode(projectPath, StandardCharsets.UTF_8);

		return AUTHORIZED_CLIENT
				.target(getBaseUrl() + "/repo/local-checkout?user=" + user + "&path=" + projectPath)
				.request()
				.get();
	}

	public static Response insertTestCase(String username, String envName, String testCaseName,
			String filePath, String funcName) {
		CreateTestDTO createTestDTO = new CreateTestDTO();
		createTestDTO.setUser(username);
		createTestDTO.setEnvironment(envName);
		createTestDTO.setName(testCaseName);
		createTestDTO.setUut(filePath);
		createTestDTO.setSut(funcName);

		return AUTHORIZED_CLIENT
				.target(getBaseUrl() + "/func/newTest")
				.request()
				.post(Entity.json(createTestDTO));
	}

	public static Response duplicateTestCase(String username, String testId, String testCaseName,
			String environment, String filePath, String funcName) {
		DuplicateTestDTO duplicateTestDTO = new DuplicateTestDTO();
		duplicateTestDTO.setEnvironment(environment);
		duplicateTestDTO.setUser(username);
		duplicateTestDTO.setTestCaseId(testId);
		duplicateTestDTO.setName(testCaseName);
		duplicateTestDTO.setUut(filePath);
		duplicateTestDTO.setSut(funcName);

		return AUTHORIZED_CLIENT
				.target(getBaseUrl() + "/test/duplicate")
				.request()
				.post(Entity.json(duplicateTestDTO));
	}

	public static Response exportTest(String userName, List<ExportTestDTO> exportTestList) {
		ExportListDTO exportListDTO = new ExportListDTO();
		exportListDTO.setUser(userName);
		exportListDTO.setExportTestList(exportTestList);

		return AUTHORIZED_CLIENT
				.target(getBaseUrl() + "/test/export")
				.request()
				.post(Entity.json(exportListDTO));
	}

	public static Response importTest(String userName, String env, String filePath) {
		ImportTestDTO importTestDTO = new ImportTestDTO();
		importTestDTO.setUser(userName);
		importTestDTO.setEnv(env);
		importTestDTO.setFilePath(filePath);

		return AUTHORIZED_CLIENT
				.target(getBaseUrl() + "/test/import")
				.request()
				.post(Entity.json(importTestDTO));
	}

	public static Response deleteTestCases(String username, List<String> ids) {
		TestIdsDTO testIdsDTO = new TestIdsDTO();
		testIdsDTO.setIds(ids);
		testIdsDTO.setUser(username);

		return AUTHORIZED_CLIENT
				.target(getBaseUrl() + "/test/delete")
				.request()
				.method("DELETE", Entity.json(testIdsDTO));
	}

	public static Response logon(String username, String password) {
		AccountDTO accountDTO = new AccountDTO(username, password);
		return UNAUTHORIZED_CLIENT
				.target(getBaseUrl() + "/user/logon"
//						+ "?username=" + username + "&password=" + password
				)
				.request()
				.post(Entity.json(accountDTO));
	}

	public static void logoff(String username) {
		AUTHORIZED_CLIENT.target(getBaseUrl() + "/user/logoff/" + username).request().get();
	}

	public static Response viewTest(String id, String username) {
		return AUTHORIZED_CLIENT
				.target(getBaseUrl() + "/test/view?id=" + id + "&user=" + username)
				.request()
				.get();
	}

	public static Response enterTestData(String testCaseId, String nodePath, String value, String username) {
		ModifyTestDTO modifyTestDTO = new ModifyTestDTO(testCaseId, nodePath, value, ModifyTestDTO.ChangeType.ENTER);
		modifyTestDTO.setUser(username);
		logger.debug("enter test Data");

		return AUTHORIZED_CLIENT
				.target(getBaseUrl() + "/test/modify")
				.request()
				.put(Entity.json(modifyTestDTO));
	}

	public static Response assertTestData(String testCaseId, String nodePath, String value, String username) {
		ModifyTestDTO modifyTestDTO = new ModifyTestDTO(testCaseId, nodePath, value, ModifyTestDTO.ChangeType.ASSERT);
		modifyTestDTO.setUser(username);
		return AUTHORIZED_CLIENT
				.target(getBaseUrl() + "/test/modify")
				.request()
				.put(Entity.json(modifyTestDTO));
	}

	public static Response userCodeTestData(String env, String file, String func, ObservableList<UserTypedefRow> listUserCode, String username) {
		UserCodeDTO userCodeDTO = new UserCodeDTO(env, file, func, listUserCode);
		userCodeDTO.setUser(username);
		logger.debug("HttpUtils user code test data");

		return AUTHORIZED_CLIENT
				.target(getBaseUrl() + "/usercode/createUserCodeData")
				.request()
				.put(Entity.json(userCodeDTO));
	}

	public static Response modifyUserCode(ObservableList<UserTypedefRow> listUserCode, String username) {
		ModifyUserCodeDTO dto = new ModifyUserCodeDTO(listUserCode);
		dto.setUser(username);
		logger.debug("HttpUtils modify user code data");

		return AUTHORIZED_CLIENT
				.target(getBaseUrl() + "/usercode/modifyUserCode")
				.request()
				.put(Entity.json(dto));
	}

	public static Response deleteUserCodeById(String username, List<String> Ids) {
		DeleteUserCodeIdDTO dto = new DeleteUserCodeIdDTO();
		dto.setIds(Ids);
		dto.setUser(username);
		logger.debug(dto.getIds().get(0));

		logger.debug("HttpUtils delete user code by Id");

		return AUTHORIZED_CLIENT
				.target(getBaseUrl() + "/usercode/delete")
				.request()
				.method("DELETE", Entity.json(dto));
	}

	public static Response getFuncArgument(String env, String funcName, String path) {
//		AutoGenDTO autoGenDTO = new AutoGenDTO(uut, sut, environment);
//		autoGenDTO.setUser(user);
		FunctionDTO funcdto = new FunctionDTO(env, funcName, path);
//
		return AUTHORIZED_CLIENT
				.target(getBaseUrl() + "/usercode/getArgument")
				.request()
				.post(Entity.json(funcdto));
	}

	public static Response getAllTestCaseOfAFunction(String uut, String sut, String env) {
		uut = URLEncoder.encode(uut, StandardCharsets.UTF_8);
		sut = URLEncoder.encode(sut, StandardCharsets.UTF_8);

		return AUTHORIZED_CLIENT
				.target(getBaseUrl() + "/test/getAll?uut=" + uut + "&sut=" + sut + "&env=" + env)
				.request()
				.get();
	}

	public static Response getAllTestCaseOfAEnv(String env) {
		return AUTHORIZED_CLIENT
				.target(getBaseUrl() + "/test/getAllTestCase?env=" + env)
				.request()
				.get();
	}

	public static Response getAllUserCodeOfAfunction(String uut, String sut, String env) {
		FunctionDTO funcdto = new FunctionDTO(env, uut, sut);

		return AUTHORIZED_CLIENT
				.target(getBaseUrl() + "/usercode/getAll")
				.request()
				.post(Entity.json(funcdto));
	}
	public static Response viewSource(@NotNull String file, @Nullable String function, @NotNull String env) {
		file = URLEncoder.encode(file, StandardCharsets.UTF_8);

		String url;
		if (function != null) {
			function = URLEncoder.encode(function, StandardCharsets.UTF_8);
			url = getBaseUrl() + "/func/source?file=" + file + "&function=" + function + "&environment=" + env;
		} else {
			url = getBaseUrl() + "/func/source?file=" + file + "&environment=" + env;
		}

		return AUTHORIZED_CLIENT
				.target(url)
				.request()
				.get();
	}

	public static Response runTest(String user, String testId) {
		return AUTHORIZED_CLIENT
				.target(getBaseUrl() + "/test/run?user=" + user + "&id=" + testId)
				.request()
				.get();
	}

	public static Response getAllEnv() {
		return AUTHORIZED_CLIENT
				.target(getBaseUrl() + "/env/list")
				.request()
				.get();
	}

	public static Response autoGenTestData(String user, String uut, String sut, String environment, String strategy) {
		AutoGenDTO autoGenDTO = new AutoGenDTO(uut, sut, strategy, environment);
		autoGenDTO.setUser(user);

		return AUTHORIZED_CLIENT
				.target(getBaseUrl() + "/func/gen")
				.request()
				.post(Entity.json(autoGenDTO));
	}

	public static Response viewCoverage(String username, List<String> ids) {
		TestIdsDTO testIdsDTO = new TestIdsDTO();
		testIdsDTO.setIds(ids);
		testIdsDTO.setUser(username);

		return AUTHORIZED_CLIENT
				.target(getBaseUrl() + "/func/viewCoverage")
				.request()
				.post(Entity.json(testIdsDTO));
	}

	public static Response getOverviewReport(String user, String name, List<Source> uutList) {
		user = URLEncoder.encode(user, StandardCharsets.UTF_8);
		name = URLEncoder.encode(name, StandardCharsets.UTF_8);

		GenerateOverviewReportDTO generateOverviewReportDTO = new GenerateOverviewReportDTO(name, uutList);
		generateOverviewReportDTO.setUser(user);

		return AUTHORIZED_CLIENT
				.target(getBaseUrl() + "/env/genReport")
				.request()
				.post(Entity.json(generateOverviewReportDTO));
	}

}
