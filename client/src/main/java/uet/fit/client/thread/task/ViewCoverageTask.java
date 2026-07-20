package uet.fit.client.thread.task;

import com.google.gson.Gson;
import uet.fit.client.thread.LogableCallAPITask;
import uet.fit.client.utils.HttpUtils;
import uet.fit.dto.test.MultiTestResultDTO;

import javax.ws.rs.core.Response;
import java.util.List;

public class ViewCoverageTask extends LogableCallAPITask<MultiTestResultDTO> {

	private final List<String> ids;

	public ViewCoverageTask(String username, List<String> ids) {
		super(username);
		this.ids = ids;
	}

	@Override
	protected Response request() throws Exception {
		return HttpUtils.viewCoverage(username, ids);
	}

	@Override
	protected MultiTestResultDTO toEntity(String json) {
		return new Gson().fromJson(json, MultiTestResultDTO.class);
	}
}
