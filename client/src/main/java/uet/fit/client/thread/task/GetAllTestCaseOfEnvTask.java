package uet.fit.client.thread.task;

import com.google.gson.Gson;
import uet.fit.client.thread.CallAPITask;
import uet.fit.client.utils.HttpUtils;
import uet.fit.dto.test.TestListDTO;

import javax.ws.rs.core.Response;

public class GetAllTestCaseOfEnvTask extends CallAPITask<TestListDTO> {

	private final String env;

	public GetAllTestCaseOfEnvTask(String env) {
		this.env = env;
	}

	@Override
	protected Response request() throws Exception {
		return HttpUtils.getAllTestCaseOfAEnv(env);
	}

	@Override
	protected TestListDTO toEntity(String json) {
		return new Gson().fromJson(json, TestListDTO.class);
	}
}
