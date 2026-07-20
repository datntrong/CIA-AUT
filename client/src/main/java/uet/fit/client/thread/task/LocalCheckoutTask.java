package uet.fit.client.thread.task;

import com.google.gson.Gson;
import uet.fit.client.thread.LogableCallAPITask;
import uet.fit.client.utils.HttpUtils;
import uet.fit.dto.repo.LocalCheckoutDTO;

import javax.ws.rs.core.Response;

public class LocalCheckoutTask extends LogableCallAPITask<LocalCheckoutDTO> {

	private final String projectPath;

	public LocalCheckoutTask(String user, String projectPath) {
		super(user);
		this.projectPath = projectPath;
	}

	@Override
	protected Response request() throws Exception {
		return HttpUtils.localCheckout(username, projectPath);
	}

	@Override
	protected LocalCheckoutDTO toEntity(String json) {
		return new Gson().fromJson(json, LocalCheckoutDTO.class);
	}
}
