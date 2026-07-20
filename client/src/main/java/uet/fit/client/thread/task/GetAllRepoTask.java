package uet.fit.client.thread.task;

import com.google.gson.Gson;
import uet.fit.client.thread.CallAPITask;
import uet.fit.client.utils.HttpUtils;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

public class GetAllRepoTask extends CallAPITask<List<String>> {

	private final String url, version;

	public GetAllRepoTask(String url, String version) {
		this.url = url;
		this.version = version;
	}

	@Override
	protected Response request() throws Exception {
		return HttpUtils.getAllRepos(url, version);
	}

	@Override
	protected List<String> toEntity(String json) {
		return Arrays.asList(new Gson().fromJson(json, String[].class));
	}
}
