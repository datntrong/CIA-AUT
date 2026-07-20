package uet.fit.client.thread.task;

import com.google.gson.Gson;
import uet.fit.client.thread.LogableCallAPITask;
import uet.fit.client.utils.HttpUtils;
import uet.fit.dto.env.EnvironmentDTO;
import uet.fit.dto.env.InstrumentDTO;

import javax.ws.rs.core.Response;

public class InstrumentEnvTask extends LogableCallAPITask<InstrumentDTO> {

	private final String envName;
	private final String proPath;

	public InstrumentEnvTask(String envName, String owner, String proPath) {
		super(owner);
		this.envName = envName;
		this.proPath = proPath;
	}

	@Override
	protected Response request() throws Exception {
		return HttpUtils.instrument(envName, username, proPath);
	}

	@Override
	protected InstrumentDTO toEntity(String json) {
		return new Gson().fromJson(json, InstrumentDTO.class);
	}
}
