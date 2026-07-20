package uet.fit.client.ui.controller.env;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import uet.fit.cia.communicate.CommitResponse;
import uet.fit.client.common.User;
import uet.fit.client.thread.AUTThread;
import uet.fit.client.thread.task.CheckoutTask;
import uet.fit.client.ui.UIHelper;
import uet.fit.client.ui.controller.HomeController;
import uet.fit.client.ui.controller.cia.CommitsChoosingTableController;
import uet.fit.client.ui.view.CommitChoosingTableView;
import uet.fit.client.ui.view.CreateEnvironmentView;
import uet.fit.client.ui.view.IEnvironmentBuilderStep;
import uet.fit.client.ui.view.IVersionChooser;
import uet.fit.dto.logger.LogDTO;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

import static uet.fit.client.ui.controller.cia.CommitsChoosingController.commitGetShortHash;

public class ChooseVersionController implements Initializable, IEnvironmentBuilderStep, IVersionChooser {

	@FXML
	private CommitChoosingTableView tvCommits;
	@FXML
	private Button btnBack;
	@FXML
	private Button btnNext;

	private final BooleanProperty isLoading = new SimpleBooleanProperty();

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {
		tvCommits.getController().setChooseQuantity(1);
		btnNext.disableProperty().bind(tvCommits.getController().isFullyChosen().not().or(isLoading));
		btnBack.disableProperty().bind(isLoading);
		tvCommits.disableProperty().bind(isLoading);
	}

	@Override
	public String getVersion() {
		String version = null;
		CommitResponse commit = tvCommits.getController().getChosenCommits().stream().findFirst().orElse(null);
		if (commit != null) {
			version = commit.getCommitHash();
		}
		return version;
	}

	@FXML
	public void btnBack_Clicked() {
		HomeController.getInstance()
				.getPaneNewUT()
				.setEnvBuilderStep(STEP_CLONE_REPOSITORY);
	}

	@FXML
	public void btnNext_Clicked() {
		CommitResponse commit = tvCommits.getController().getChosenCommits().stream().findFirst().orElse(null);

		if (commit == null) return;

		Alert alert = UIHelper.showAlert(Alert.AlertType.CONFIRMATION,
				"Unit Test",
				"Create test environment for " + commitGetShortHash(commit),
				commit.getMessage());

		Optional<ButtonType> action = alert.showAndWait();

		if (action.isPresent() && action.get() == ButtonType.OK) {
			final String username = User.getInstance().getUsername();
			final String url = HomeController.getInstance()
					.getPaneNewUT()
					.getCurrentRepositoryUrl();
			final String version = getVersion();
			final User.Git gitUser = User.getInstance().getGit();
			CheckoutTask task = new CheckoutTask(username, url, version, gitUser);
			task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
				@Override
				public void handle(WorkerStateEvent workerStateEvent) {
					isLoading.set(false);

					CreateEnvironmentView view = HomeController.getInstance()
							.getPaneNewUT()
							.setEnvBuilderStep(STEP_CONFIG_ENVIRONMENT);
					view.setProFiles(task.getValue());

					HomeController.getInstance()
							.getPaneNewUT()
							.setCurrentRepositoryVersion(version);
				}
			});
			task.setOnFailed(new EventHandler<WorkerStateEvent>() {
				@Override
				public void handle(WorkerStateEvent workerStateEvent) {
					isLoading.set(false);

					String logContent = task.getException().getMessage();
					HomeController.getInstance().log(LogDTO.TYPE_ERR, logContent);
					UIHelper.showErrorAlert("Cannot clone repository", logContent)
							.showAndWait();
				}
			});
			new AUTThread(task).start();

			isLoading.set(true);
		}
	}

	public void setGitInfo(String username, String gitUrl, String gitUsername, String gitPassword) {
		final CommitsChoosingTableController controller = tvCommits.getController();
		controller.setGitInfo(username, gitUrl, gitUsername, gitPassword);
		controller.reload();
	}

	@Override
	public void clearState() {
		tvCommits.getController().clearState();
	}
}