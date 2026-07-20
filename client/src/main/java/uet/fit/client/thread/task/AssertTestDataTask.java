package uet.fit.client.thread.task;

import uet.fit.client.thread.LogableCallAPITask;
import uet.fit.client.utils.HttpUtils;
import uet.fit.dto.deserializer.TestDataImporter;
import uet.fit.dto.test.data.TestDataDTO;

import javax.ws.rs.core.Response;

public class AssertTestDataTask extends LogableCallAPITask<TestDataDTO> {

	private final String testCaseId;
	private final String path;
	private final String value;

	public AssertTestDataTask(String testCaseId, String path, String value, String username) {
		super(username);
		this.testCaseId = testCaseId;
		this.path = path;
		this.value = value;
	}

	@Override
	protected Response request() throws Exception {
		return HttpUtils.assertTestData(testCaseId, path, value, username);
	}

	@Override
	protected TestDataDTO toEntity(String json) {
		return TestDataImporter.fromJson(json);
	}
}
