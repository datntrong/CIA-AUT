package uet.fit.client.thread.task;

import uet.fit.client.thread.LogableCallAPITask;
import uet.fit.client.utils.HttpUtils;

import javax.ws.rs.core.Response;

public class CloneRepositoryTask extends LogableCallAPITask<String> {

	private final String url;
	private final String gitUser, gitPassword;

	public CloneRepositoryTask(String user, String url, String gitUser, String gitPassword) {
		super(user);
		this.url = url;
		this.gitUser = gitUser;
		this.gitPassword = gitPassword;
	}

	@Override
	protected Response request() throws Exception {
		return HttpUtils.cloneRepository(username, url, gitUser, gitPassword);
	}

	@Override
	protected String toEntity(String json) {
		// TODO review this
		return json;
	}
}
