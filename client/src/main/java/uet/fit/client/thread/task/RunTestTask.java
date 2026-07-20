package uet.fit.client.thread.task;

import com.google.gson.Gson;
import uet.fit.client.thread.LogableCallAPITask;
import uet.fit.client.utils.HttpUtils;
import uet.fit.dto.test.SingleTestResultDTO;

import javax.ws.rs.core.Response;

public class RunTestTask extends LogableCallAPITask<SingleTestResultDTO> {

	private final String testId;

	public RunTestTask(String username, String testId) {
		super(username);
		this.testId = testId;
	}

	@Override
	protected Response request() throws Exception {
		return HttpUtils.runTest(username, testId);
	}

	@Override
	protected SingleTestResultDTO toEntity(String json) {
		return new Gson().fromJson(json, SingleTestResultDTO.class);
	}
}
