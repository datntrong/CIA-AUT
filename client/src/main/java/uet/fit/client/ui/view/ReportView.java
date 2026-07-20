package uet.fit.client.ui.view;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import uet.fit.client.ui.controller.test.ReportViewController;

import java.io.IOException;

public class ReportView extends AnchorPane {

	private final ReportViewController controller;

	public ReportView() {
		FXMLLoader loader = new FXMLLoader();
		loader.setRoot(this);

		try {
			loader.load(getClass().getResourceAsStream("/fxml/ReportView.fxml"));
			controller = loader.getController();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void setContent(String content) {
		controller.loadContentToReportView(content);
	}

}
