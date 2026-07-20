package uet.fit.client.thread.task;

import com.google.gson.Gson;
import uet.fit.client.thread.CallAPITask;
import uet.fit.client.thread.LogableCallAPITask;
import uet.fit.client.utils.HttpUtils;
import uet.fit.dto.UserDTO.DeleteUserCodeIdDTO;
import uet.fit.dto.env.EnvironmentListDTO;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

public class DeleteUserCodeTask extends LogableCallAPITask<DeleteUserCodeIdDTO> {
	private List<String> Ids;

	public DeleteUserCodeTask(String username ,List<String> Ids) {
		super(username);
		this.Ids = Ids;
	}

	@Override
	protected Response request() throws Exception {
		return HttpUtils.deleteUserCodeById(username, Ids);
	}

	@Override
	protected DeleteUserCodeIdDTO toEntity(String json) {
		return new Gson().fromJson(json, DeleteUserCodeIdDTO.class);
	}


}
