package uet.fit.client.ui.controller.env;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.util.Callback;
import uet.fit.client.common.User;
import uet.fit.client.thread.AUTThread;
import uet.fit.client.thread.task.CreateEnvTask;
import uet.fit.client.ui.UIHelper;
import uet.fit.client.ui.controller.BaseController;
import uet.fit.client.ui.controller.HomeController;
import uet.fit.client.ui.controller.cia.CIAMainViewController;
import uet.fit.client.ui.view.IEnvironmentBuilderStep;
import uet.fit.client.ui.view.NewUnitTestView;
import uet.fit.client.utils.CalUtils;
import uet.fit.dto.env.EnvironmentDTO;
import uet.fit.dto.logger.LogDTO;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class CreateEnvironmentController implements Initializable, IEnvironmentBuilderStep {

	@FXML
	private Button btnBack;
	@FXML
	private Button btnCreate;
	@FXML
	private Button btnBrowser;
	@FXML
	private ComboBox<String> cbCoverage;
	@FXML
	private TextField tfName;
	@FXML
	private TextField tfProPath;

	private final SimpleBooleanProperty validEnvNameProperty = new SimpleBooleanProperty(false);

	private BooleanBinding creatableEnvBinding;

	private final BooleanProperty isLoading = new SimpleBooleanProperty();

	private List<String> proFiles;

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {
		cbCoverage.getItems().add("STATEMENT");
//		cbCoverage.getItems().add("BRANCH");
//		cbCoverage.getItems().add("MC/DC");
		cbCoverage.setValue("STATEMENT");

		tfName.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observableValue, String old, String name) {
				final String trimmedName = name.trim();
				boolean validName = CalUtils.validateName(trimmedName);
				validEnvNameProperty.set(validName);
			}
		});

		validEnvNameProperty.addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observableValue, Boolean old, Boolean isValid) {
				styleInvalidEnvName(isValid);
			}
		});

		BooleanBinding validProjectBinding = tfProPath.textProperty().isNotEmpty();
		creatableEnvBinding = Bindings.and(validEnvNameProperty, validProjectBinding);
		btnCreate.disableProperty().bind(creatableEnvBinding.not().or(isLoading));
		btnBack.disableProperty().bind(isLoading);
		btnBrowser.disableProperty().bind(isLoading);
		tfName.disableProperty().bind(isLoading);
		cbCoverage.disableProperty().bind(isLoading);
	}

	@FXML
	public void chooseButton_Clicked(ActionEvent actionEvent) {
		UIHelper.showChooseProPath(proFiles, new Callback<String, Void>() {
			@Override
			public Void call(String s) {
				updateProPath(s);
				return null;
			}
		});
	}

	public void createButton_Clicked(ActionEvent actionEvent) {
		String environmentName = tfName.getText().trim();
		String username = User.getInstance().getUsername();
		String proPath = tfProPath.getText();
		String proFileName = new File(proPath).getName();
		String coverageType = cbCoverage.getSelectionModel().getSelectedItem();

		NewUnitTestView newUnitTestView = HomeController.getInstance().getPaneNewUT();
		String url = newUnitTestView.getCurrentRepositoryUrl();
		String version = newUnitTestView.getCurrentRepositoryVersion();

		final long startTime = System.currentTimeMillis();

		CreateEnvTask task = new CreateEnvTask(environmentName, username, coverageType, proPath, url, version);
		task.setOnSucceeded(workerStateEvent -> {
			isLoading.set(false);

			long time = (System.currentTimeMillis() - startTime) / 1000;

			BaseController.getInstance().unlockPane();

			String logContent = "" ;
			HomeController.getInstance().log(LogDTO.TYPE_INF, "Successfully created " + environmentName);
			Alert alert = UIHelper.showAlert(Alert.AlertType.INFORMATION, "SUCCESS",
					String.format("Created new environment cost %ds", time), logContent);
			alert.showAndWait();

			final EnvironmentDTO environmentDTO = task.getValue();
			HomeController.getInstance().clearPreviousState();
			HomeController.getInstance().enableAllProperty();

			String commit = uet.fit.util.Utils.shortenCommitHash(version);
			final String project = String.format("%s (%s)", commit, proFileName);
			HomeController.getInstance().openEnvironment(environmentDTO, project, username);

			// set pro file path for cia
			final User.Git git = User.getInstance().getGit();
			CIAMainViewController.getInstance()
					.initialize(username, url, git.getName(), git.getPassword());
		});

		task.setOnFailed(workerStateEvent -> {
			isLoading.set(false);

			BaseController.getInstance().unlockPane();

			String logContent = task.getException().getMessage();
			HomeController.getInstance().log(LogDTO.TYPE_ERR, logContent);
			Alert alert = UIHelper.showErrorAlert("Created new environment failed.", logContent);
			alert.showAndWait();

			HomeController.getInstance().enableAllProperty();
		});

		new AUTThread(task).start();

		isLoading.set(true);

		HomeController.getInstance().disableAllProperty();
		BaseController.getInstance().lockPane();
	}

	@FXML
	public void btnBack_Clicked() {
		HomeController.getInstance()
				.getPaneNewUT()
				.setEnvBuilderStep(STEP_CHOOSE_VERSION);
	}

	private void updateProPath(String path) {
		tfProPath.setText(path);
	}

	private void styleInvalidEnvName(boolean isValid) {
		if (isValid || tfName.getText().isEmpty()) {
			tfName.setStyle("");
		} else {
			tfName.setStyle("-fx-border-color: red");
		}
	}

	@Override
	public void clearState() {
		tfName.clear();
		tfProPath.clear();
		cbCoverage.setValue("STATEMENT");
	}

	public void setProFiles(List<String> files) {
		proFiles = files;
		if (proFiles.size() == 1)
			tfProPath.setText(proFiles.get(0));
	}
}
