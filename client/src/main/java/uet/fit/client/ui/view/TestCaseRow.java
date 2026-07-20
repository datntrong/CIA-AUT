package uet.fit.client.ui.view;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableRow;
import javafx.scene.image.ImageView;
import uet.fit.client.ui.controller.test.TestController;
import uet.fit.dto.test.TestRow;

public class TestCaseRow extends TableRow<TestRow> {

	@Override
	protected void updateItem(TestRow testRow, boolean empty) {
		super.updateItem(testRow, empty);

		ContextMenu contextMenu = null;

		if (testRow == null || empty) {
			setText(null);
			setGraphic(null);
		} else {
			contextMenu = new ContextMenu();

			MenuItem deleteItem = new MenuItem("Delete");
			deleteItem.setGraphic(new ImageView("/images/173-bin.png"));
			deleteItem.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent actionEvent) {
					TestController.getInstance().deleteButton_Clicked();
				}
			});
			contextMenu.getItems().add(deleteItem);

			MenuItem duplicateItem = new MenuItem("Duplicate");
			duplicateItem.setGraphic(new ImageView("/images/045-copy.png"));
			duplicateItem.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent actionEvent) {
					TestController.getInstance().duplicate_Clicked();
				}
			});
			contextMenu.getItems().add(duplicateItem);

			MenuItem exportItem = new MenuItem("Export");
			exportItem.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent actionEvent) {
					TestController.getInstance().export_Clicked();
				}
			});
			contextMenu.getItems().add(exportItem);

			MenuItem importItem = new MenuItem("Import");
			importItem.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent actionEvent) {
					TestController.getInstance().import_Clicked();
				}
			});
			contextMenu.getItems().add(importItem);

			MenuItem runItem = new MenuItem("Run");
			runItem.setGraphic(new ImageView("/images/285-play3.png"));
			runItem.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent actionEvent) {
					TestController.getInstance().run_Clicked();
				}
			});
			contextMenu.getItems().add(runItem);

			MenuItem viewCoverageItem = new MenuItem("View Coverage");
			viewCoverageItem.setGraphic(new ImageView("/images/155-pie-chart.png"));
			viewCoverageItem.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent actionEvent) {
					TestController.getInstance().viewCoverage_Clicked();
				}
			});
			contextMenu.getItems().add(viewCoverageItem);
		}
		setContextMenu(contextMenu);
	}
}
