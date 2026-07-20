package uet.fit.client.ui.controller.dialogs;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import uet.fit.client.common.User;
import uet.fit.client.ui.view.LoginView;

public class GitLoginDialog extends AbstractLoginDialog {

	public GitLoginDialog() {
		super();
		setTitle("Git Login");
	}

	@Override
	protected void customView(LoginView loginView, Button btnOk) {
		Node pane = generateBanner();
		loginView.setBanner(pane);

		TextField usernameEditor = loginView.getUsernameEditor();
		usernameEditor.setText(User.getInstance().getGit().getName());
		TextField passwordEditor = loginView.getPasswordEditor();
		passwordEditor.setText(User.getInstance().getGit().getPassword());
	}

	private Node generateBanner() {
		ImageView bitbucket = new ImageView("/images/Bitbucket@2x-blue.png");
		bitbucket.setFitHeight(36);
		bitbucket.setPreserveRatio(true);
		ImageView git = new ImageView("/images/git.png");
		git.setFitHeight(40);
		git.setPreserveRatio(true);
		HBox pane = new HBox(git, bitbucket);
		pane.setAlignment(Pos.CENTER);
		HBox.setMargin(bitbucket, new Insets(0, 0, 0, 12));
		pane.setSpacing(8.0);
		return pane;
	}
}
