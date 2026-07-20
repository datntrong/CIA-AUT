package uet.fit.client.ui.view;

import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import uet.fit.client.ui.UIHelper;
import uet.fit.client.ui.controller.component.LoginController;

public class LoginView extends VBox implements IEnvironmentBuilderStep {

	private final LoginController controller;

	public LoginView() {
		controller = UIHelper.loadFXML(this, "/fxml/dialogs/Login.fxml");
		controller.disableProperty().bind(disableProperty());
	}

	public void setBanner(Node node) {
		controller.setBanner(node);
	}

	public TextField getUsernameEditor() {
		return controller.getUsernameEditor();
	}

	public TextField getPasswordEditor() {
		return controller.getPasswordEditor();
	}

	public String getUsername() {
		return controller.getUsername();
	}

	public String getPassword() {
		return controller.getPassword();
	}

	@Override
	public void clearState() {
		controller.clearState();
	}
}
