package uet.fit.client.thread.task;

import com.google.gson.Gson;
import uet.fit.client.thread.LogableCallAPITask;
import uet.fit.client.utils.HttpUtils;
import uet.fit.dto.env.EnvironmentDTO;

import javax.ws.rs.core.Response;

public class GetEnvByNameTask extends LogableCallAPITask<EnvironmentDTO> {

	private final String envName, gitUser, gitPassword;

	public GetEnvByNameTask(String username, String envName, String gitUser, String gitPassword) {
		super(username);
		this.gitPassword = gitPassword;
		this.gitUser = gitUser;
		this.envName = envName;
	}

	@Override
	protected Response request() throws Exception {
		return HttpUtils.getEnv(username, envName, gitUser, gitPassword);
	}

	@Override
	protected EnvironmentDTO toEntity(String json) {
		return new Gson().fromJson(json, EnvironmentDTO.class);
	}
}
