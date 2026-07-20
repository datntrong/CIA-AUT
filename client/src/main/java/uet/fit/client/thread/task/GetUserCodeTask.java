package uet.fit.client.thread.task;

import com.google.gson.Gson;
import uet.fit.client.thread.CallAPITask;
import uet.fit.client.thread.LogableCallAPITask;
import uet.fit.client.utils.HttpUtils;
import uet.fit.dto.UserDTO.UserCodeDTO;
import uet.fit.dto.UserDTO.UserTypedefRow;
import uet.fit.dto.test.TestListDTO;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

public class GetUserCodeTask extends CallAPITask<List<UserTypedefRow>> {
	private String uut;
	private String sut;
	 private String env;

	public GetUserCodeTask(String uut, String sut, String env) {
		this.uut = uut;
		this.sut = sut;
		this.env = env;
	}

	public void setUut(String uut) {
		this.uut = uut;
	}

	public void setSut(String sut) {
		this.sut = sut;
	}

	public void setEnv(String env) {
		this.env = env;
	}

	public String getUut() {
		return uut;
	}

	public String getSut() {
		return sut;
	}

	public String getEnv() {
		return env;
	}

	@Override
	protected Response request() throws Exception {
		return HttpUtils.getAllUserCodeOfAfunction(uut,sut,env);
	}

	@Override
	protected List<UserTypedefRow> toEntity(String json) {
		return Arrays.asList(new Gson().fromJson(json, UserTypedefRow[].class));
	}
}
