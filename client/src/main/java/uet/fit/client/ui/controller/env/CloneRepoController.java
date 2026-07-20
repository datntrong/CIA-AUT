package uet.fit.client.ui.controller.env;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import org.jetbrains.annotations.NotNull;
import uet.fit.client.common.User;
import uet.fit.client.thread.AUTThread;
import uet.fit.client.thread.task.CloneRepositoryTask;
import uet.fit.client.ui.UIHelper;
import uet.fit.client.ui.controller.HomeController;
import uet.fit.client.ui.controller.cia.RepositoryInputComboBox;
import uet.fit.client.ui.view.IEnvironmentBuilderStep;
import uet.fit.client.ui.view.LoginView;
import uet.fit.client.utils.HttpUtils;
import uet.fit.dto.logger.LogDTO;

import java.net.URL;
import java.util.ResourceBundle;

public class CloneRepoController implements Initializable, IEnvironmentBuilderStep {

	@FXML
	private Button btnBrowse;
	@FXML
	private LoginView vGitLogin;
	@FXML
	private RepositoryInputComboBox tfRepoURL;
	@FXML
	private Button btnNext;

	private final BooleanProperty isLoading = new SimpleBooleanProperty();

	private OnSuccessListener onCloneSuccess;

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {
		BooleanBinding emptyUrl = tfRepoURL.textProperty().isEmpty();
		btnNext.disableProperty().bind(emptyUrl.or(isLoading));
		tfRepoURL.disableProperty().bind(isLoading);
		vGitLogin.disableProperty().bind(isLoading);
		if (!HttpUtils.isBaseUrlLocalhost())
			btnBrowse.setDisable(true);
		else
			btnBrowse.disableProperty().bind(isLoading);
	}

	public void setOnCloneSuccess(OnSuccessListener onCloneSuccess) {
		this.onCloneSuccess = onCloneSuccess;
	}

	@FXML
	public void cloneRepository() {
		final String username = User.getInstance().getUsername();
		String url = tfRepoURL.getText();
		String gitUser = vGitLogin.getUsername();
		String gitPassword = vGitLogin.getPassword();
		CloneRepositoryTask task = new CloneRepositoryTask(username, url, gitUser, gitPassword);
		task.setOnSucceeded(workerStateEvent -> {
			isLoading.set(false);
			saveAccount(gitUser, gitPassword);
			onCloneSuccess.run(username, new User.Git(gitUser, gitPassword), url, task.getValue());
		});
		task.setOnFailed(workerStateEvent -> {
			isLoading.set(false);

			String logContent = task.getException().getMessage();
			HomeController.getInstance().log(LogDTO.TYPE_ERR, logContent);
			UIHelper.showErrorAlert("Cannot clone repository", logContent)
					.showAndWait();
		});
		new AUTThread(task).start();
		isLoading.set(true);
	}

//	@FXML
//	public void btnCIA_Clicked(ActionEvent actionEvent) {
//		final String username = User.getInstance().getUsername();
//		String url = tfRepoURL.getText();
//		String gitUser = vGitLogin.getUsername();
//		String gitPassword = vGitLogin.getPassword();
//		CloneRepositoryTask task = new CloneRepositoryTask(username, url, gitUser, gitPassword);
//		task.setOnSucceeded(workerStateEvent -> {
//			isLoading.set(false);
//
//			saveAccount(gitUser, gitPassword);
//
//			CIAMainViewController.getInstance()
//					.initialize(username, task.getValue(), url, gitUser, gitPassword);
//			BaseController.getInstance().moveToCiaTab();
//		});
//		task.setOnFailed(workerStateEvent -> {
//			isLoading.set(false);
//
//			String logContent = task.getException().getMessage();
//			HomeController.getInstance().log(LogDTO.TYPE_ERR, logContent);
//			UIHelper.showErrorAlert("Cannot clone repository", logContent)
//					.showAndWait();
//		});
//		new AUTThread(task).start();
//		isLoading.set(true);
//	}
//
//	@FXML
//	public void btnTest_Clicked(ActionEvent actionEvent) {
//		final String username = User.getInstance().getUsername();
//		String url = tfRepoURL.getText();
//		String gitUser = vGitLogin.getUsername();
//		String gitPassword = vGitLogin.getPassword();
//		CloneRepositoryTask task = new CloneRepositoryTask(username, url, gitUser, gitPassword);
//		task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
//			@Override
//			public void handle(WorkerStateEvent workerStateEvent) {
//				isLoading.set(false);
//
//				saveAccount(gitUser, gitPassword);
//
//				ChooseVersionView chooseVersionView = HomeController.getInstance().setEnvBuilderStep(STEP_CHOOSE_VERSION);
//				chooseVersionView.getController().setGitInfo(username, url, gitUser, gitPassword);
//			}
//		});
//		task.setOnFailed(new EventHandler<WorkerStateEvent>() {
//			@Override
//			public void handle(WorkerStateEvent workerStateEvent) {
//				isLoading.set(false);
//
//				String logContent = task.getException().getMessage();
//				HomeController.getInstance().log(LogDTO.TYPE_ERR, logContent);
//				UIHelper.showErrorAlert("Cannot clone repository", logContent)
//						.showAndWait();
//			}
//		});
//		new AUTThread(task).start();
//
//		isLoading.set(true);
//	}

	private void saveAccount(String gitUsername, String gitPassword) {
		User.Git gitAccount = User.getInstance().getGit();
		gitAccount.setName(gitUsername);
		gitAccount.setPassword(gitPassword);
	}

	public void setOnBrowse(EventHandler<MouseEvent> onBrowser) {
		btnBrowse.setOnMouseClicked(onBrowser);
	}

	@Override
	public void clearState() {
		tfRepoURL.clear();
		vGitLogin.clearState();
	}

	public interface OnSuccessListener {
		void run(@NotNull String username, @NotNull User.Git git,
				@NotNull String url, @NotNull String projectPath);
	}
}
