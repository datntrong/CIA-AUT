package uet.fit.client.thread.task;

import com.google.gson.Gson;
import uet.fit.client.thread.LogableCallAPITask;
import uet.fit.client.utils.HttpUtils;
import uet.fit.dto.env.EnvironmentDTO;

import javax.ws.rs.core.Response;

public class CreateEnvTask extends LogableCallAPITask<EnvironmentDTO> {

	private final String envName;
	private final String coverageType;
	private final String proPath;
	private final String url;
	private final String version;

	public CreateEnvTask(String envName, String owner, String coverageType, String proPath, String url, String version) {
		super(owner);
		this.envName = envName;
		this.coverageType = coverageType;
		this.proPath = proPath;
		this.url = url;
		this.version = version;
	}

	@Override
	protected Response request() throws Exception {
		return HttpUtils.createNewEnv(envName, username, coverageType, proPath, url, version);
	}

	@Override
	protected EnvironmentDTO toEntity(String json) {
		return new Gson().fromJson(json, EnvironmentDTO.class);
	}
}
