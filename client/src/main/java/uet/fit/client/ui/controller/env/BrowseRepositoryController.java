package uet.fit.client.ui.controller.env;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import uet.fit.client.common.User;
import uet.fit.client.thread.AUTThread;
import uet.fit.client.thread.task.CheckoutTask;
import uet.fit.client.thread.task.LocalCheckoutTask;
import uet.fit.client.ui.UIHelper;
import uet.fit.client.ui.controller.HomeController;
import uet.fit.client.ui.view.CreateEnvironmentView;
import uet.fit.client.ui.view.IEnvironmentBuilderStep;
import uet.fit.client.ui.view.NewUnitTestView;
import uet.fit.dto.logger.LogDTO;
import uet.fit.dto.repo.LocalCheckoutDTO;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;

public class BrowseRepositoryController implements Initializable, IEnvironmentBuilderStep {

	@FXML
	private TextField tfProjectPath;
	@FXML
	private Button btnBrowser;
	@FXML
	private Button btnClone;
	@FXML
	private Button btnNext;

	private final BooleanProperty isLoading = new SimpleBooleanProperty();

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {
		btnBrowser.disableProperty().bind(isLoading);
		btnClone.disableProperty().bind(isLoading);
		btnNext.disableProperty().bind(isLoading.or(tfProjectPath.textProperty().isEmpty()));
		tfProjectPath.disableProperty().bind(isLoading);
	}

	@FXML
	public void btnNext_Clicked() {
		final String username = User.getInstance().getUsername();
		final String projectPath = tfProjectPath.getText();
		LocalCheckoutTask task = new LocalCheckoutTask(username, projectPath);
		task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent workerStateEvent) {
				isLoading.set(false);

				LocalCheckoutDTO dto = task.getValue();

				NewUnitTestView newUnitTestView = HomeController.getInstance().getPaneNewUT();

				CreateEnvironmentView view = newUnitTestView.setEnvBuilderStep(STEP_CONFIG_ENVIRONMENT);
				view.setProFiles(Arrays.asList(dto.getProFiles()));

				newUnitTestView.setCurrentRepositoryUrl(dto.getGitUrl());
				newUnitTestView.setCurrentRepositoryVersion(dto.getCommit());
			}
		});
		task.setOnFailed(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent workerStateEvent) {
				isLoading.set(false);

				String logContent = task.getException().getMessage();
				HomeController.getInstance().log(LogDTO.TYPE_ERR, logContent);
				UIHelper.showErrorAlert("Cannot checkout repository", logContent)
						.showAndWait();
			}
		});
		new AUTThread(task).start();

		isLoading.set(true);
	}

	@FXML
	public void btnClone_Clicked() {
		HomeController.getInstance()
				.getPaneNewUT()
				.setEnvBuilderStep(IEnvironmentBuilderStep.STEP_CLONE_REPOSITORY);
	}

	@FXML
	public void chooseButton_Clicked() {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		File selectedDirectory = directoryChooser.showDialog(UIHelper.getPrimaryStage());
		if (selectedDirectory != null) {
			tfProjectPath.setText(selectedDirectory.getAbsolutePath());
		}
	}

	@Override
	public void clearState() {
		tfProjectPath.clear();
	}
}
