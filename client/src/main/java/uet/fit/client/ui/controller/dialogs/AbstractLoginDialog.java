package uet.fit.client.ui.controller.dialogs;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import uet.fit.client.ui.UIHelper;
import uet.fit.client.ui.view.LoginView;

public abstract class AbstractLoginDialog extends Dialog<AbstractLoginDialog.Account> {

	public AbstractLoginDialog() {
		DialogPane dialogPane = getDialogPane();

		dialogPane.getStylesheets().add("/css/style.css");

		LoginView loginView = new LoginView();
		dialogPane.setContent(loginView);

		// Set the button types.
		ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
		ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
		dialogPane.getButtonTypes().addAll(okButtonType, cancelButtonType);

		Button okButton = (Button) dialogPane.lookupButton(okButtonType);
		customView(loginView, okButton);

		this.setResultConverter((var1x) -> {
			ButtonBar.ButtonData var2 = var1x == null ? null : var1x.getButtonData();
			if (var2 == ButtonBar.ButtonData.OK_DONE) {
				String password = loginView.getPassword();
				String username = loginView.getUsername();
				return new Account(username, password);
			} else return null;
		});

		initOwner(UIHelper.getPrimaryStage());
	}

	protected void customView(LoginView loginView, Button okBtn) {

	}

	public static class Account {

		private String username, password;

		public Account(String username, String password) {
			this.username = username;
			this.password = password;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}
	}
}
