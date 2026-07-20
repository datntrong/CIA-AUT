package uet.fit.client.thread.task;

import com.google.gson.Gson;
import uet.fit.client.thread.LogableCallAPITask;
import uet.fit.client.utils.HttpUtils;
import uet.fit.dto.env.DeletedEnvironmentDTO;

import javax.ws.rs.core.Response;
import java.util.List;

public class DeleteEnviromentTask extends LogableCallAPITask<DeletedEnvironmentDTO> {

	private List<String> environmentNames;

	public DeleteEnviromentTask(String username, List<String> environmentNames){
		super(username);
		this.environmentNames = environmentNames;
	}

	@Override
	protected Response request() throws Exception {
		return HttpUtils.deleteEnv(username, environmentNames);
	}

	@Override
	protected DeletedEnvironmentDTO toEntity(String json) {
		return new Gson().fromJson(json, DeletedEnvironmentDTO.class);
	}
}
