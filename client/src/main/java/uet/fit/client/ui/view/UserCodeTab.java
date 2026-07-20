package uet.fit.client.ui.view;

import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import uet.fit.client.ui.controller.component.UserCodeTabController;
import uet.fit.dto.UserDTO.UserTypedefRow;

import java.io.IOException;

public class UserCodeTab extends Tab {
	private final UserCodeTabController controller;
	private final String env;
	private final String uut;
	private static UserCodeTab instance;

	public String getUut() {
		return uut;
	}

	public String getEnv() {
		return env;
	}

	public UserCodeTab(String funcName, String env, String uut) {
		this.env = env;
		this.uut = uut;
		FXMLLoader loader = new FXMLLoader();
		loader.setRoot(this);
		this.setText(funcName + "/" + uut + "/" + env);
		try {
			loader.load(getClass().getResourceAsStream("/fxml/UserCodeTab.fxml"));
			controller = loader.getController();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static UserCodeTab getInstance(String funcName,String env, String uut) {
		if(instance == null) {
			return new UserCodeTab(funcName,env,uut);
		}
		if(instance.getEnv().equals(env) && instance.getUut().equals(uut) && instance.getText().equals(funcName)) {
			return instance;
		} else {
			return new UserCodeTab(funcName,env,uut);
		}
	}
	public ObservableList<UserTypedefRow> getUserDataList() {
		return controller.getUserInputList();
	}

	public UserCodeTabController getController(){
		return controller;
	}
}
