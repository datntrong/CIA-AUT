package uet.fit.client.ui.view;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import uet.fit.client.ui.controller.component.LogViewController;
import uet.fit.dto.logger.LogDTO;

import java.io.IOException;

public class LogView extends AnchorPane {

	private final LogViewController controller;

	public LogView() {
		FXMLLoader loader = new FXMLLoader();
		loader.setRoot(this);

		try {
			loader.load(getClass().getResourceAsStream("/fxml/LogView.fxml"));
			controller = loader.getController();

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void debug(String message) {
		controller.log(message, LogDTO.TYPE_DEB);
	}
	public void info(String message) {
		controller.log(message, LogDTO.TYPE_INF);
	}
	public void error(String message) {
		controller.log(message, LogDTO.TYPE_ERR);
	}
	public void log(String message, byte type) {
		controller.log(message, type);
	}

}
