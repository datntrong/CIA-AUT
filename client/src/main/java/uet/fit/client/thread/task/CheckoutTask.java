package uet.fit.client.thread.task;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import uet.fit.client.common.User;
import uet.fit.client.thread.LogableCallAPITask;
import uet.fit.client.utils.HttpUtils;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

public class CheckoutTask extends LogableCallAPITask<List<String>> {

	private final String url;
	private final String version;
	private final String gitUsername;
	private final String gitPassword;

	public CheckoutTask(String user, String url, String version, User.Git gitUser) {
		super(user);
		this.url = url;
		this.version = version;
		this.gitUsername = gitUser.getName();
		this.gitPassword = gitUser.getPassword();
	}

	@Override
	protected Response request() throws Exception {
		return HttpUtils.checkout(username, url, version, gitUsername, gitPassword);
	}

	@Override
	protected List<String> toEntity(String json) {
		return Arrays.asList(new Gson().fromJson(json, String[].class));
	}
}
