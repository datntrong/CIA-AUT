package uet.fit.client.ui.controller.dialogs;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;
import uet.fit.dto.test.DeletedTestEntry;
import uet.fit.dto.test.TestRow;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class StatusDeleteController implements Initializable {

	@FXML
	private TableView<DeletedTestWithNameEntry> table;
	@FXML
	private TableColumn<DeletedTestWithNameEntry, String> name;
	@FXML
	private TableColumn<DeletedTestWithNameEntry, String> status;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		name.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<DeletedTestWithNameEntry, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(TableColumn.CellDataFeatures<DeletedTestWithNameEntry, String> features) {
				SimpleStringProperty nameProperty = new SimpleStringProperty();
				if (features.getValue() != null) {
					String name = features.getValue().getName();
					nameProperty.set(name);
				}
				return nameProperty;
			}
		});
		status.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<DeletedTestWithNameEntry, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(TableColumn.CellDataFeatures<DeletedTestWithNameEntry, String> features) {
				SimpleStringProperty statusProperty = new SimpleStringProperty();
				if (features.getValue() != null) {
					String status = features.getValue().getStatus().name();
					status = status.toLowerCase().replace("_", " ");
					statusProperty.set(status);
				}
				return statusProperty;
			}
		});
	}

	public void showStatus(List<DeletedTestEntry> deletedTestEntries, List<TestRow> testRowList) {
		table.getItems().clear();
		List<TestRow> rows = new ArrayList<>(testRowList);
		for (DeletedTestEntry entry : deletedTestEntries) {
			String id = entry.getId();
			TestRow row = rows.stream()
					.filter(r -> r.getId().equals(id))
					.findFirst()
					.orElse(null);
			if (row != null) {
				DeletedTestWithNameEntry withNameEntry = new DeletedTestWithNameEntry(entry, row.getName());
				rows.remove(row);
				table.getItems().addAll(withNameEntry);
			}
		}
	}

	private static class DeletedTestWithNameEntry extends DeletedTestEntry {

		private final String name;

		public DeletedTestWithNameEntry(DeletedTestEntry entry, String name) {
			super(entry.getId(), entry.getStatus());
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}
}
