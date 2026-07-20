package uet.fit.client.thread.task;

import uet.fit.client.thread.LogableCallAPITask;
import uet.fit.client.utils.HttpUtils;
import uet.fit.dto.test.ExportTestDTO;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

public class ExportTestTask extends LogableCallAPITask<String> {

	private List<ExportTestDTO> exportTestList = new ArrayList<>();

	public ExportTestTask(String userName, List<ExportTestDTO> exportTestList) {
		super(userName);
		this.exportTestList = exportTestList;

	}

	@Override
	protected Response request() throws Exception {
		return HttpUtils.exportTest(username, exportTestList);
	}

	@Override
	protected String toEntity(String json) {
		return json;
	}
}
