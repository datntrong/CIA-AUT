package uet.fit.client.thread.task;

import uet.fit.client.thread.LogableCallAPITask;
import uet.fit.client.utils.HttpUtils;
import uet.fit.dto.deserializer.TestDataImporter;
import uet.fit.dto.test.data.TestDataDTO;

import javax.ws.rs.core.Response;

public class DuplicateTestCaseTask extends LogableCallAPITask<TestDataDTO> {

	private final String testId;
	private final String testCaseName;
	private final String filePath;
	private final String funcName;
	private final String environment;

	public DuplicateTestCaseTask(String username, String testId, String testCaseName,
			String environment, String filePath, String funcName) {
		super(username);
		this.testId = testId;
		this.testCaseName = testCaseName;
		this.filePath = filePath;
		this.funcName = funcName;
		this.environment = environment;
	}

	@Override
	protected Response request() throws Exception {
		return HttpUtils.duplicateTestCase(username, testId, testCaseName, environment, filePath, funcName);
	}

	@Override
	protected TestDataDTO toEntity(String json) {
		return TestDataImporter.fromJson(json);
	}
}
