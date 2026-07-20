package uet.fit.client.thread.task;

import com.google.gson.Gson;
import uet.fit.client.thread.CallAPITask;
import uet.fit.client.utils.HttpUtils;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

public class GetFucArgumentTask extends CallAPITask<List<String>> {
	String env;
	String functionPath;
	String funcName;

	public GetFucArgumentTask(String env, String functionPath, String funcName) {
		this.env = env;
		this.functionPath = functionPath;
		this.funcName = funcName;
	}

	public String getEnv() {
		return env;
	}

	public void setEnv(String env) {
		this.env = env;
	}

	public String getFunctionPath() {
		return functionPath;
	}

	public void setFunctionPath(String functionPath) {
		this.functionPath = functionPath;
	}

	public String getFuncName() {
		return funcName;
	}

	public void setFuncName(String funcName) {
		this.funcName = funcName;
	}

	@Override
	protected Response request() throws Exception {
		return HttpUtils.getFuncArgument(env,funcName,functionPath);
	}

	@Override
	protected List<String> toEntity(String json) {
		return Arrays.asList(new Gson().fromJson(json, String[].class));
	}
}
