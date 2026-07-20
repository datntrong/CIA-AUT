package uet.fit.client.thread.task;

import com.google.gson.Gson;
import uet.fit.client.thread.CallAPITask;
import uet.fit.client.utils.HttpUtils;
import uet.fit.dto.env.EnvironmentListDTO;

import javax.ws.rs.core.Response;

public class GetAllEnvTask extends CallAPITask<EnvironmentListDTO> {

	@Override
	protected Response request() throws Exception {
		return HttpUtils.getAllEnv();
	}

	@Override
	protected EnvironmentListDTO toEntity(String json) {
		return new Gson().fromJson(json, EnvironmentListDTO.class);
	}
}
