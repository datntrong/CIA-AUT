package uet.fit.client.thread.task;

import com.google.gson.Gson;
import uet.fit.client.thread.CallAPITask;
import uet.fit.client.utils.HttpUtils;
import uet.fit.dto.test.TestListDTO;

import javax.ws.rs.core.Response;

public class ListTestCaseTask extends CallAPITask<TestListDTO> {

	private final String uut;
	private final String sut;
	private final String env;

	public ListTestCaseTask(String uut, String sut, String env) {
		this.uut = uut;
		this.sut = sut;
		this.env = env;
	}

	@Override
	protected Response request() throws Exception {
		return HttpUtils.getAllTestCaseOfAFunction(uut, sut, env);
	}

	@Override
	protected TestListDTO toEntity(String json) {
		return new Gson().fromJson(json, TestListDTO.class);
	}
}
