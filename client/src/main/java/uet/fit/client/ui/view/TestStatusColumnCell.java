package uet.fit.client.ui.view;

import javafx.scene.control.TableCell;
import uet.fit.dto.test.TestRow;

public class TestStatusColumnCell extends TableCell<TestRow, String> {

	@Override
	protected void updateItem(String status, boolean empty) {
		super.updateItem(status, empty);

		if (status == null || empty) {
			setText(null);
			setGraphic(null);
		} else if (status.equals("N/A")) {
			setText(null);
			setGraphic(null);
		} else if (!status.contains("/")) {
			setText(status);
			setGraphic(null);
		} else {
			setText(status);

			String strPass = status.substring(0, status.indexOf("/"));
			String strTotal = status.substring(status.indexOf("/") + 1);

			if (strTotal.equals("0")) {
				setText("success");
			} else {
				if (strPass.equals(strTotal)) {
					setStyle("-fx-background-color: lightgreen");
				} else if (strPass.equals("0")) {
					setStyle("-fx-background-color: #EA9999");
				} else {
					setStyle("-fx-background-color: #FFE599");
				}
			}
		}
	}
}