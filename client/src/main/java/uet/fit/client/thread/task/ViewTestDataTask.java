package uet.fit.client.thread.task;

import uet.fit.client.thread.LogableCallAPITask;
import uet.fit.client.utils.HttpUtils;
import uet.fit.dto.deserializer.TestDataImporter;
import uet.fit.dto.test.data.TestDataDTO;

import javax.ws.rs.core.Response;

public class ViewTestDataTask extends LogableCallAPITask<TestDataDTO> {

	private final String testCaseId;

	public ViewTestDataTask(String testCaseId, String username) {
		super(username);
		this.testCaseId = testCaseId;
	}

	@Override
	protected Response request() throws Exception {
		return HttpUtils.viewTest(testCaseId, username);
	}

	@Override
	protected TestDataDTO toEntity(String json) {
		return TestDataImporter.fromJson(json);
	}
}
