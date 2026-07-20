package uet.fit.client.thread.task;

import com.google.gson.Gson;
import javafx.collections.ObservableList;
import uet.fit.client.thread.LogableCallAPITask;
import uet.fit.client.utils.HttpUtils;
import uet.fit.dto.UserDTO.UserTypedefRow;
import uet.fit.dto.test.GenResultDTO;

import javax.ws.rs.core.Response;

public class AutoGenTestDataTask extends LogableCallAPITask<GenResultDTO> {

	private final String uut;
	private final String sut;
	private final String env;

	private final String strategy;

	public AutoGenTestDataTask(String user, String uut, String sut, String env, String strategy) {
		super(user);
		this.uut = uut;
		this.sut = sut;
		this.env = env;
		this.strategy = strategy;
	}

	@Override
	protected Response request() throws Exception {
		return HttpUtils.autoGenTestData(username, uut, sut, env, strategy);
	}

	@Override
	protected GenResultDTO toEntity(String json) {
		return new Gson().fromJson(json, GenResultDTO.class);
	}
}
