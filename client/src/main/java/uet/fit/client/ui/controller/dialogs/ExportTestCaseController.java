package uet.fit.client.ui.controller.dialogs;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import uet.fit.client.common.User;
import uet.fit.client.thread.AUTThread;
import uet.fit.client.thread.task.ExportTestTask;
import uet.fit.client.thread.task.ListTestCaseTask;
import uet.fit.client.ui.UIHelper;
import uet.fit.client.ui.controller.test.TestController;
import uet.fit.client.ui.obj.ChooseTestCase;
import uet.fit.dto.env.INavigableNode;
import uet.fit.dto.logger.LogDTO;
import uet.fit.dto.test.ExportTestDTO;
import uet.fit.dto.test.TestListDTO;
import uet.fit.dto.test.TestRow;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ExportTestCaseController implements Initializable {

	@FXML
	private TextField pathExportFile;
	@FXML
	private Button browserButton;
	@FXML
	private CheckBox selectAll;

	@FXML
	private TableView<ChooseTestCase> chooseTestTable;
	@FXML
	private TableColumn<ChooseTestCase, String> colChoose;
	@FXML
	private TableColumn<ChooseTestCase, String> colName;
	@FXML
	private TableColumn<ChooseTestCase, String> colStatus;
	@FXML
	private TableColumn<ChooseTestCase, String> colCoverage;
	@FXML
	private TableColumn<ChooseTestCase, String> colAuthor;
	@FXML
	private TableColumn<ChooseTestCase, String> colFunc;

	private ObservableList<ChooseTestCase> chooseTestList = FXCollections.observableArrayList();


	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {

		selectAll.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean t1) {

				chooseTestList = chooseTestTable.getItems();
				for(ChooseTestCase testCase : chooseTestList) {
					if(selectAll.isSelected()) testCase.getChoose().setSelected(true);
					else testCase.getChoose().setSelected(false);
				}
			}
		});

		colName.setCellValueFactory(new PropertyValueFactory<ChooseTestCase, String>("name"));
		colStatus.setCellValueFactory(new PropertyValueFactory<ChooseTestCase, String>("status"));
		colCoverage.setCellValueFactory(new PropertyValueFactory<ChooseTestCase, String>("coverage"));
		colAuthor.setCellValueFactory(new PropertyValueFactory<ChooseTestCase, String>("author"));
		colFunc.setCellValueFactory(new PropertyValueFactory<ChooseTestCase, String>("sut"));
		colChoose.setCellValueFactory(new PropertyValueFactory<ChooseTestCase, String>("choose"));

		chooseTestTable.setItems(chooseTestList);
	}

	public void loadAllTests(String env, List<TreeItem<INavigableNode>> uutList) {
		for(TreeItem<INavigableNode> uut : uutList) {
			for(TreeItem<INavigableNode> sut : uut.getChildren()) {
				ListTestCaseTask task = new ListTestCaseTask(uut.getValue().getTitle(), sut.getValue().getTitle(), env);
				task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
					@Override
					public void handle(WorkerStateEvent workerStateEvent) {
						TestListDTO testListDTO = task.getValue();
						for(TestRow testRow : testListDTO.getList()) {
							ChooseTestCase testCase = new ChooseTestCase(testRow.getName(), testRow.getStatus(),
									String.valueOf(testRow.getCoverage()), testRow.getOwner(), testRow.getId(),
									testListDTO.getUut(), testListDTO.getSut());
							chooseTestList.add(testCase);
						}
					}
				});
				task.setOnFailed(new EventHandler<WorkerStateEvent>() {
					@Override
					public void handle(WorkerStateEvent workerStateEvent) {
						String logContent = task.getException().getMessage();
						TestController.getInstance().logGeneral(LogDTO.TYPE_ERR, logContent);
					}
				});
				new AUTThread(task).start();
			}
		}
		chooseTestTable.setItems(chooseTestList);
	}

	public void exportSelectedTest(String uut, String sut, List<TestRow> testRows) {
		for(TestRow testRow : testRows) {
			ChooseTestCase testCase = new ChooseTestCase(testRow.getName(), testRow.getStatus(),
					String.valueOf(testRow.getCoverage()), testRow.getOwner(), testRow.getId(),
					uut, sut);
			chooseTestList.add(testCase);
		}
	}

	public void export_Clicked() {
		String user = User.getInstance().getUsername();
		List<ExportTestDTO> exportTestList = new ArrayList<>();
		if(pathExportFile.getText().equals("")) {
			UIHelper.showAlert(Alert.AlertType.INFORMATION, "ERROR", "Select directory to export test cases")
					.showAndWait();
		}
		else {
			for (ChooseTestCase testCase : chooseTestList) {
				if (testCase.getChoose().isSelected()) {
					ExportTestDTO exportTest = new ExportTestDTO();
					exportTest.setTestCaseId(testCase.getId());
					exportTest.setTestCaseName(testCase.getName());
					exportTest.setSut(testCase.getSut());
					exportTest.setUut(testCase.getUut());
					exportTest.setFilePath(pathExportFile.getText());
					exportTestList.add(exportTest);
					System.out.println(testCase.getName());
				}
			}
			ExportTestTask task = new ExportTestTask(user, exportTestList);
			task.setOnSucceeded(workerStateEvent -> {
				String notify = task.getValue();
				TestController.getInstance().logGeneral(LogDTO.TYPE_INF, notify);
				UIHelper.showAlert(Alert.AlertType.INFORMATION, "SUCCESS", "Export test cases completed!")
						.showAndWait();
			});
			task.setOnFailed(workerStateEvent -> {
				String logContent = task.getException().getMessage();
				TestController.getInstance().logGeneral(LogDTO.TYPE_ERR, logContent);
			});
			new AUTThread(task).start();
		}
	}

	public void browser_Clicked() {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		File selectedDirectory = directoryChooser.showDialog(UIHelper.getPrimaryStage());
		if (selectedDirectory != null) {
			pathExportFile.setText(selectedDirectory.getAbsolutePath());
		}
	}

}


