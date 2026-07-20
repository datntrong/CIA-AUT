package uet.fit.client.thread.task;

import com.google.gson.Gson;
import javafx.collections.ObservableList;
import uet.fit.client.thread.LogableCallAPITask;
import uet.fit.client.utils.HttpUtils;
import uet.fit.dto.UserDTO.DeleteUserCodeIdDTO;
import uet.fit.dto.UserDTO.ModifyUserCodeDTO;
import uet.fit.dto.UserDTO.UserTypedefRow;

import javax.ws.rs.core.Response;

public class ModifyUserCodeTask extends LogableCallAPITask<ModifyUserCodeDTO> {
	private ObservableList<UserTypedefRow> listModifiedUserCode;

	public ModifyUserCodeTask(ObservableList<UserTypedefRow> listModifiedUserCode, String username) {
		super(username);
		this.listModifiedUserCode = listModifiedUserCode;
	}

	@Override
	protected Response request() throws Exception {
		return HttpUtils.modifyUserCode(listModifiedUserCode,username);
	}

	@Override
	protected ModifyUserCodeDTO toEntity(String json) {
		return new Gson().fromJson(json, ModifyUserCodeDTO.class);
	}
}
