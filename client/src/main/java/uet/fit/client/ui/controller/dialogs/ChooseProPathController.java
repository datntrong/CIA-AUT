package uet.fit.client.ui.controller.dialogs;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ChooseProPathController implements Initializable {

	@FXML
	private Button cancelButton;
	@FXML
	private Button selectButton;
	@FXML
	private ListView<String> lvProPath;

	private Callback<String, Void> callback;

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {
		lvProPath.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
			@Override
			public ListCell<String> call(ListView<String> listView) {
				ListCell<String> cell = new Cell();

				cell.setOnMouseClicked(new EventHandler<MouseEvent>() {
					@Override
					public void handle(MouseEvent mouseEvent) {
						if (mouseEvent.getClickCount() == 2) {
							if (choose()) {
								Stage stage = (Stage) lvProPath.getScene().getWindow();
								stage.close();
							}
						}
					}
				});

				return cell;
			}
		});
	}

	public void setLvProPath(List<String> value) {
		lvProPath.getItems().setAll(value);
	}

	public void selectButton_Clicked() {
		if (choose()) {
			Stage stage = (Stage) selectButton.getScene().getWindow();
			stage.close();
		}
	}

	private boolean choose() {
		String proPath = lvProPath.getSelectionModel().getSelectedItem();
		if (proPath != null) {
			callback.call(proPath);
			return true;
		} else {
			return false;
		}
	}

	public void cancelButton_Clicked() {
		Stage stage = (Stage) cancelButton.getScene().getWindow();
		stage.close();
	}

	public void setCallback(Callback<String, Void> callback) {
		this.callback = callback;
	}

	static class Cell extends ListCell<String> {
		@Override
		protected void updateItem(String s, boolean b) {
			super.updateItem(s, b);
			setText(s);
			setGraphic(null);
		}
	}
}
