package uet.fit.client.thread.task;

import com.google.gson.Gson;
import uet.fit.client.thread.CallAPITask;
import uet.fit.client.thread.LogableCallAPITask;
import uet.fit.client.utils.HttpUtils;
import uet.fit.dto.ReportDTO;
import uet.fit.dto.env.EnvironmentDTO;
import uet.fit.dto.env.Source;

import javax.ws.rs.core.Response;
import java.util.List;

public class GenerateOverviewReportTask extends LogableCallAPITask<ReportDTO> {

	private final String envName;

	private final List<Source> uutList;

	public GenerateOverviewReportTask(String username, String envName, List<Source> uutList) {
		super(username);
		this.envName = envName;
		this.uutList = uutList;
	}

	@Override
	protected Response request() throws Exception {
		return HttpUtils.getOverviewReport(username, envName, uutList);
	}

	@Override
	protected ReportDTO toEntity(String json) {
		return new Gson().fromJson(json, ReportDTO.class);
	}
}
