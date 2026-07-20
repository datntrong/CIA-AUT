package uet.fit.client.thread.task;

import com.google.gson.Gson;
import uet.fit.client.thread.LogableCallAPITask;
import uet.fit.client.utils.HttpUtils;
import uet.fit.dto.test.DeletedTestsDTO;

import javax.ws.rs.core.Response;
import java.util.List;

public class DeleteTestCasesTask extends LogableCallAPITask<DeletedTestsDTO> {

	private final List<String> ids;

	public DeleteTestCasesTask(String username, List<String> ids) {
		super(username);
		this.ids = ids;
	}


	@Override
	protected Response request() throws Exception {
		return HttpUtils.deleteTestCases(username, ids);
	}

	@Override
	protected DeletedTestsDTO toEntity(String json) {
		return new Gson().fromJson(json, DeletedTestsDTO.class);
	}
}
