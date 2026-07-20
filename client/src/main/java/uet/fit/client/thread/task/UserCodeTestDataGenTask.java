package uet.fit.client.thread.task;

import javafx.collections.ObservableList;
import uet.fit.client.thread.LogableCallAPITask;
import uet.fit.client.utils.HttpUtils;
import uet.fit.dto.UserDTO.UserTypedefRow;
import uet.fit.dto.deserializer.TestDataImporter;
import uet.fit.dto.test.data.TestDataDTO;

import javax.ws.rs.core.Response;

public class UserCodeTestDataGenTask  extends LogableCallAPITask<TestDataDTO> {

	private final String env;
	private final String file;
	private final String func;
	private ObservableList<UserTypedefRow> listParameterUserCode;

	public UserCodeTestDataGenTask(String env, String file, String func, ObservableList<UserTypedefRow> listParameterUserCode, String username) {
		super(username);
		this.listParameterUserCode = listParameterUserCode;
		this.env = env;
		this.file = file;
		this.func = func;
	}

	@Override
	protected Response request() throws Exception {
		return HttpUtils.userCodeTestData(env, file, func, listParameterUserCode, username);
	}

	@Override
	protected TestDataDTO toEntity(String json) {
		return TestDataImporter.fromJson(json);
	}
}

