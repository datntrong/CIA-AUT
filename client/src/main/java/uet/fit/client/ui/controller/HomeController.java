package uet.fit.client.ui.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.client.common.RecentEnvironmentList;
import uet.fit.client.common.User;
import uet.fit.client.thread.AUTThread;
import uet.fit.client.thread.task.GetAllEnvTask;
import uet.fit.client.thread.task.GetEnvByNameTask;
import uet.fit.client.ui.UIHelper;
import uet.fit.client.ui.controller.cia.CIAMainViewController;
import uet.fit.client.ui.controller.dialogs.AbstractLoginDialog;
import uet.fit.client.ui.controller.dialogs.GitLoginDialog;
import uet.fit.client.ui.controller.test.TestController;
import uet.fit.client.ui.view.CloneRepoView;
import uet.fit.client.ui.view.LogView;
import uet.fit.client.ui.view.NewUnitTestView;
import uet.fit.dto.env.EnvironmentDTO;
import uet.fit.dto.env.EnvironmentListDTO;
import uet.fit.dto.env.EnvironmentRow;
import uet.fit.dto.env.RecentEnvironmentRow;
import uet.fit.dto.logger.LogDTO;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.ResourceBundle;

public class HomeController implements Initializable, IPreviousStateCleanable {

	private static final Logger LOGGER = LoggerFactory.getLogger(HomeController.class);

	private static HomeController instance;

	private final ObservableList<EnvironmentRow> observableEnvList = FXCollections.observableArrayList();

	@FXML
	private NewUnitTestView paneNewUT;
	@FXML
	private SplitPane spContainer;
	@FXML
	private Button btnRefresh;
	@FXML
	private TabPane tabPane;
	@FXML
	private LogView taLog;
	@FXML
	private TextField tfSearch;
	@FXML
	private TableView<RecentEnvironmentRow> tbRecentEnvs;
	@FXML
	private TableColumn<RecentEnvironmentRow, String> colNameRecent;
	@FXML
	private TableColumn<RecentEnvironmentRow, String> colCoverageTypeRecent;
	@FXML
	private TableColumn<RecentEnvironmentRow, String> colProjectRecent;
	@FXML
	private TableColumn<RecentEnvironmentRow, String> colAuthorRecent;
	@FXML
	private TableColumn<RecentEnvironmentRow, String> colLastOpened;
	@FXML
	private TableView<EnvironmentRow> tbAllEnv;
	@FXML
	private TableColumn<EnvironmentRow, String> colName;
	@FXML
	private TableColumn<EnvironmentRow, String> colCoverageType;
	@FXML
	private TableColumn<EnvironmentRow, String> colProject;
	@FXML
	private TableColumn<EnvironmentRow, String> colAuthor;

	public static HomeController getInstance() {
		return instance;
	}

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {
		HomeController.instance = this;

		HBox.setHgrow(tfSearch, Priority.ALWAYS);

		VBox.setVgrow(spContainer, Priority.ALWAYS);
		VBox.setVgrow(tbAllEnv, Priority.ALWAYS);
		VBox.setVgrow(tbRecentEnvs, Priority.ALWAYS);

		colName.setCellValueFactory(new PropertyValueFactory<>("name"));
		colCoverageType.setCellValueFactory(new PropertyValueFactory<>("coverageType"));
		colProject.setCellValueFactory(new PropertyValueFactory<>("project"));
		colAuthor.setCellValueFactory(new AuthorColumnCellValueFactory<>());

		FilteredList<EnvironmentRow> filteredEnvironments = new FilteredList<>(observableEnvList, s -> true);
		tfSearch.textProperty().addListener((observableValue, oldValue, newValue) -> {
			if (newValue == null || newValue.trim().isEmpty()) {
				filteredEnvironments.setPredicate(env -> true);
			} else {
				filteredEnvironments.setPredicate(env -> env.getName().contains(newValue.trim()));
			}
		});

		tbAllEnv.setItems(filteredEnvironments);

		tbAllEnv.setOnMouseClicked(mouseEvent -> {
			if (mouseEvent.getClickCount() == 2) {
				if (tbAllEnv.getSelectionModel().getSelectedItem() != null) {
					EnvironmentRow selectedEnvironment = tbAllEnv.getSelectionModel().getSelectedItem();
					LOGGER.info("Open " + selectedEnvironment);
					getEnvironment(selectedEnvironment);
				}
			}
		});

		colNameRecent.setCellValueFactory(new PropertyValueFactory<>("name"));
		colCoverageTypeRecent.setCellValueFactory(new PropertyValueFactory<>("coverageType"));
		colProjectRecent.setCellValueFactory(new PropertyValueFactory<>("project"));
		colAuthorRecent.setCellValueFactory(new AuthorColumnCellValueFactory<>());
		colLastOpened.setCellValueFactory(new PropertyValueFactory<>("lastOpened"));

		tbRecentEnvs.setOnMouseClicked(mouseEvent -> {
			if (mouseEvent.getClickCount() == 2) {
				if (tbRecentEnvs.getSelectionModel().getSelectedItem() != null) {
					EnvironmentRow selectedEnvironment = tbRecentEnvs.getSelectionModel().getSelectedItem();
					LOGGER.info("Open " + selectedEnvironment);
					getEnvironment(selectedEnvironment);
				}
			}
		});
	}

	public void retrieveData() {
		retrieveAllEnvs();
	}

	@FXML
	public void refreshAllEnvironments() {
		observableEnvList.clear();
		retrieveAllEnvs();
	}

	@FXML
	public void ciaExistRepository() {

	}

	@FXML
	public void cloneCIA(CloneRepoView.CloneEvent event) {
		String username = event.getUsername();
		String url = event.getUrl();
		User.Git git = event.getGit();
		CIAMainViewController.getInstance()
				.initialize(username, url, git.getName(), git.getPassword());
		BaseController.getInstance().moveToCiaTab();
	}

	private void retrieveAllEnvs() {
		GetAllEnvTask task = new GetAllEnvTask();
		task.setOnSucceeded(workerStateEvent -> {
			String logContent = "Successfully retrieve all environments";
			log(LogDTO.TYPE_INF, logContent);
			EnvironmentListDTO environmentListDTO = task.getValue();
			observableEnvList.setAll(environmentListDTO);
			RecentEnvironmentList.getInstance().filterDeleted(environmentListDTO);
			retrieveRecentEnvs(environmentListDTO);
		});
		task.setOnFailed(workerStateEvent -> {
			String logContent = task.getException().getMessage();
			log(LogDTO.TYPE_ERR, logContent);
			UIHelper.showErrorAlert("Cannot get all environments!", logContent)
					.showAndWait();
		});
		new AUTThread(task).start();
	}

	private void getEnvironment(EnvironmentRow environmentRow) {
		GitLoginDialog loginDialog = new GitLoginDialog();
		Optional<AbstractLoginDialog.Account> result = loginDialog.showAndWait();

		if (result.isPresent()) {
			disableAllProperty();

			String environment = environmentRow.getName();
			String username = User.getInstance().getUsername();

			final long startTime = System.currentTimeMillis();

			String gitUsername = result.get().getUsername();
			String gitPassword = result.get().getPassword();

			GetEnvByNameTask task = new GetEnvByNameTask(username, environment, gitUsername, gitPassword);
			task.setOnSucceeded(workerStateEvent -> {
				long time = (System.currentTimeMillis() - startTime) / 1000;

				BaseController.getInstance().unlockPane();
				String logContent = "Successfully get environment" ;
				log(LogDTO.TYPE_INF, logContent);
				final EnvironmentDTO environmentDTO = task.getValue();
				openEnvironment(environmentDTO, environmentRow.getProject(), environmentRow.getAuthor());
				UIHelper.showAlert(Alert.AlertType.INFORMATION, "SUCCESSFULLY OPEN ENVIRONMENT",
						String.format("Successfully open environment cost %ds", time)).showAndWait();
				enableAllProperty();

				// set pro file path for cia
				String gitUrl = environmentDTO.getGitUrl();
				CIAMainViewController.getInstance()
						.initialize(username, gitUrl, gitUsername, gitPassword);

				User.Git gitAccount = User.getInstance().getGit();
				gitAccount.setName(gitUsername);
				gitAccount.setPassword(gitPassword);
			});
			task.setOnFailed(workerStateEvent -> {
				BaseController.getInstance().unlockPane();

				String logContent = task.getException().getMessage();
				log(LogDTO.TYPE_ERR, logContent);
				enableAllProperty();
				Alert alert = UIHelper.showErrorAlert("Cannot open this environment.", logContent);
				alert.showAndWait();
			});
			new AUTThread(task).start();

			BaseController.getInstance().lockPane();
		}
	}

	public void openEnvironment(EnvironmentDTO root, String project, String author) {
		String name = root.getName();
		String coverageType = root.getCoverageType();
		String time = LocalDateTime.now().format(RecentEnvironmentList.DATE_TIME_FORMATTER);
		RecentEnvironmentRow recentEnvironment = new RecentEnvironmentRow(name, coverageType, project, author, time);

		RecentEnvironmentList.getInstance().addEnv(recentEnvironment);

		TestController.getInstance().clearPreviousState();
		BaseController.getInstance().moveToTestTab();
		TestController.getInstance().showProjectTree(root);

		RecentEnvironmentList.getInstance().toJson();

		tbRecentEnvs.getItems().setAll(RecentEnvironmentList.getInstance());
	}

	private void retrieveRecentEnvs(EnvironmentListDTO envList) {
		RecentEnvironmentList recentList = RecentEnvironmentList.getInstance();
		recentList.removeIf(re -> envList.stream().noneMatch(e -> e.getName().equals(re.getName())));
		recentList.toJson();
		tbRecentEnvs.getItems().setAll(recentList);
	}

	public NewUnitTestView getPaneNewUT() {
		return paneNewUT;
	}

	public void log(byte type, String message) {
		taLog.log(message, type);
	}

	public void disableAllProperty() {
		btnRefresh.setDisable(true);
		tbAllEnv.setDisable(true);
		tbRecentEnvs.setDisable(true);
		tabPane.setDisable(true);
	}

	public void enableAllProperty() {
		btnRefresh.setDisable(false);
		tbAllEnv.setDisable(false);
		tbRecentEnvs.setDisable(false);
		tabPane.setDisable(false);
	}

	@Override
	public void clearPreviousState() {
		tfSearch.clear();
		paneNewUT.clearPreviousState();
	}

	private static class AuthorColumnCellValueFactory<T extends EnvironmentRow> implements Callback<TableColumn.CellDataFeatures<T, String>, ObservableValue<String>> {
		@Override
		public ObservableValue<String> call(TableColumn.CellDataFeatures<T, String> cellDataFeatures) {
			EnvironmentRow row = cellDataFeatures.getValue();
			SimpleStringProperty property = new SimpleStringProperty();
			String author = row.getAuthor();
			String username = User.getInstance().getUsername();
			if (username.equals(author)) {
				property.set("You");
			} else {
				property.set(author);
			}
			return property;
		}
	}
	public void updateTbRecentEnvs(String name){
		tbRecentEnvs.getItems().removeIf(i -> i.getName().equals(name));
		observableEnvList.clear();
		retrieveAllEnvs();
	}
}
