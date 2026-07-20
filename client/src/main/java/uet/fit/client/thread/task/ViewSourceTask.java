package uet.fit.client.thread.task;

import com.google.gson.Gson;
import uet.fit.client.thread.CallAPITask;
import uet.fit.client.utils.HttpUtils;
import uet.fit.dto.func.ViewSourceDTO;
import uet.fit.dto.test.TestListDTO;

import javax.ws.rs.core.Response;

public class ViewSourceTask extends CallAPITask<ViewSourceDTO> {

	private final String file;
	private final String function;
	private final String env;

	public ViewSourceTask(String file, String function, String env) {
		this.file = file;
		this.function = function;
		this.env = env;
	}

	@Override
	protected Response request() throws Exception {
		return HttpUtils.viewSource(file, function, env);
	}

	@Override
	protected ViewSourceDTO toEntity(String json) {
		return new Gson().fromJson(json, ViewSourceDTO.class);
	}
}
