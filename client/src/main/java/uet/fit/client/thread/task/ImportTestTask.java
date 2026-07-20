package uet.fit.client.thread.task;

import com.google.gson.Gson;
import uet.fit.client.thread.LogableCallAPITask;
import uet.fit.client.utils.HttpUtils;
import uet.fit.dto.deserializer.TestDataImporter;
import uet.fit.dto.test.TestDataImportDTO;
import uet.fit.dto.test.data.TestDataDTO;

import javax.ws.rs.core.Response;

public class ImportTestTask extends LogableCallAPITask<TestDataImportDTO> {

	private String environment;
	private String filePath;

	public ImportTestTask(String username, String environment, String filePath) {
		super(username);
		this.environment = environment;
		this.filePath = filePath;
	}

	@Override
	protected Response request() throws Exception {
		return HttpUtils.importTest(username, environment, filePath);
	}

	@Override
	protected TestDataImportDTO toEntity(String json) {
		return new Gson().fromJson(json, TestDataImportDTO.class);
	}
}
