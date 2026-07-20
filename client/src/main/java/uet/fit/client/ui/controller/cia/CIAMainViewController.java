package uet.fit.client.ui.controller.cia;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CIAMainViewController {
	private static @Nullable CIAMainViewController instance = null;

	public static @NotNull CIAMainViewController getInstance() {
		if (instance == null) {
			try {
				final FXMLLoader loader = new FXMLLoader(CIAMainViewController.class
						.getResource("/fxml/cia/CIAMainView.fxml"));
				loader.load();
				return instance = loader.getController();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return instance;
	}

	@FXML private @NotNull SplitPane content;

	@FXML private @NotNull TabPane tpCIA;
	@FXML private @NotNull Tab tabCommits;
	@FXML private @NotNull Tab tabResults;
	@FXML private @NotNull LogsViewController logsViewController;

	private @NotNull String username = "";
	private @NotNull String gitUrl = "";
	private @NotNull String gitUsername = "";
	private @NotNull String gitPassword = "";

	@FXML
	private void initialize() {
	}

	LogsViewController logger() {
		return logsViewController;
	}

	public void setCommitsViewContent(@NotNull Region content) {
		tabCommits.setContent(content);
		tpCIA.getSelectionModel().select(tabCommits);
	}

	public void setResultsViewContent(@NotNull Region content) {
		tabResults.setContent(content);
		tpCIA.getSelectionModel().select(tabResults);
	}


	public void initialize(@NotNull String username, @NotNull String gitUrl,
			@NotNull String gitUsername, @NotNull String gitPassword) {
		this.username = username;
		this.gitUrl = gitUrl;
		this.gitUsername = gitUsername;
		this.gitPassword = gitPassword;

		final CommitsChoosingController commitsController = CommitsChoosingController.create();
		setCommitsViewContent(commitsController.getContent());

		// clear
		tabResults.setContent(null);
	}

	public @NotNull String getUsername() {
		return username;
	}

	public @NotNull String getGitUrl() {
		return gitUrl;
	}

	public @NotNull String getGitUsername() {
		return gitUsername;
	}

	public @NotNull String getGitPassword() {
		return gitPassword;
	}

	public @NotNull Region getContent() {
		return content;
	}

	void hideLog() {
		content.setDividerPosition(0, 1.0);
	}

	void showLog() {
		// if the log is hidden
		if (content.getDividers().get(0).getPosition() >= 0.99) {
			content.setDividerPosition(0, 0.75);
		}
	}
}
