package uet.fit.client.ui.controller.dialogs;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import uet.fit.client.common.User;
import uet.fit.client.thread.AUTThread;
import uet.fit.client.thread.task.DuplicateTestCaseTask;
import uet.fit.client.thread.task.ImportTestTask;
import uet.fit.client.ui.UIHelper;
import uet.fit.client.ui.controller.test.TestController;
import uet.fit.dto.logger.LogDTO;
import uet.fit.dto.test.TestDataImportDTO;
import uet.fit.dto.test.data.TestDataDTO;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ImportTestCaseController implements Initializable {

	@FXML
	private TextArea filePathList;

	private String env;

	List<File> selectedFileList = new ArrayList<>();

	public void browser_Clicked() {
		FileChooser fileChooser = new FileChooser();
		selectedFileList = fileChooser.showOpenMultipleDialog(UIHelper.getPrimaryStage());
		StringBuilder selectedFiles = new StringBuilder();
		for(File file : selectedFileList) {
			selectedFiles.append(file.getAbsolutePath() + '\n');
		}
		filePathList.setText(selectedFiles.toString());
	}

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {

	}

	public void import_Clicked() {
		String owner = User.getInstance().getUsername();
		for(File file : selectedFileList) {
			ImportTestTask task = new ImportTestTask(owner, env, file.getAbsolutePath());
			task.setOnSucceeded(workerStateEvent -> {
				TestDataImportDTO dataImportDTO = task.getValue();
				if(TestController.getInstance().isSelect(dataImportDTO.getUut(), dataImportDTO.getSut()));
					TestController.getInstance().insertNewTestcase(owner, dataImportDTO.getId(), dataImportDTO.getName());
			});
			task.setOnFailed(workerStateEvent -> {
				String logContent = task.getException().getMessage();
				TestController.getInstance().logGeneral(LogDTO.TYPE_ERR, "Can't import " + file.getName() + ": " + logContent);
			});
			new AUTThread(task).start();
		}
	}

	public void setEnv(String env) {
		this.env = env;
	}
}
