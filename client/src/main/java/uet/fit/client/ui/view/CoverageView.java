package uet.fit.client.ui.view;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import uet.fit.client.ui.controller.test.CoverageViewController;

import java.io.IOException;

public class CoverageView extends AnchorPane {

	private final CoverageViewController controller;

	public CoverageView() {
		FXMLLoader loader = new FXMLLoader();
		loader.setRoot(this);

		try {
			loader.load(getClass().getResourceAsStream("/fxml/CoverageView.fxml"));
			controller = loader.getController();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void setContent(String content) {
		controller.loadContentToCoverageView(content);
	}
}
