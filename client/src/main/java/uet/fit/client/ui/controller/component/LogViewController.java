package uet.fit.client.ui.controller.component;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import org.fxmisc.richtext.StyleClassedTextArea;
import uet.fit.dto.logger.LogDTO;

import java.net.URL;
import java.util.ResourceBundle;

public class LogViewController implements Initializable {

	@FXML
	private StyleClassedTextArea taLog;

	private boolean empty = true;

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {

	}

	public void log(String message, byte type) {
		if (!empty) {
			message = "\n" + message;
		}

		taLog.append(message, getStyleOfLog(type));

		// scroll to bottom
		taLog.moveTo(taLog.getLength());
		taLog.requestFollowCaret();

		empty = false;
	}

	private String getStyleOfLog(byte type) {
		switch (type) {
			case LogDTO.TYPE_ERR:
				return "red";
			case LogDTO.TYPE_INF:
				return "blue";
			default:
				return "";
		}
	}
}
