package uet.fit.client.ui.controller.dialogs;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import uet.fit.client.common.User;
import uet.fit.client.ui.view.LoginView;
import uet.fit.client.utils.CalUtils;

public class LoginDialog extends AbstractLoginDialog {

	public LoginDialog() {
		super();
		setTitle("Login");
	}

	@Override
	protected void customView(LoginView loginView, Button btnOk) {
		TextField usernameEditor = loginView.getUsernameEditor();
		usernameEditor.setText(User.getInstance().getUsername());
		usernameEditor.textProperty().addListener((observableValue, oldName, name) -> {
			if (name == null || CalUtils.validateName(name.trim())) {
				usernameEditor.setStyle("");
				btnOk.setDisable(false);
			} else {
				usernameEditor.setStyle("-fx-border-color: red");
				btnOk.setDisable(true);
			}
		});

		TextField passwordEditor = loginView.getPasswordEditor();
		passwordEditor.setText("password");
	}
}
