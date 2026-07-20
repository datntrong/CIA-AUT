package uet.fit.client.thread.task;

import uet.fit.client.thread.LogableCallAPITask;
import uet.fit.client.utils.HttpUtils;
import uet.fit.dto.deserializer.TestDataImporter;
import uet.fit.dto.test.data.TestDataDTO;

import javax.ws.rs.core.Response;

public class InsertTestCaseTask extends LogableCallAPITask<TestDataDTO> {

	private final String envName;
	private final String testCaseName;
	private final String filePath;
	private final String funcName;

	public InsertTestCaseTask(String username, String envName, String testCaseName, String filePath, String funcName) {
		super(username);
		this.envName = envName;
		this.testCaseName = testCaseName;
		this.filePath = filePath;
		this.funcName = funcName;
	}

	@Override
	protected Response request() throws Exception {
		return HttpUtils.insertTestCase(username, envName, testCaseName, filePath, funcName);
	}

	@Override
	protected TestDataDTO toEntity(String json) {
		return TestDataImporter.fromJson(json);
	}
}
