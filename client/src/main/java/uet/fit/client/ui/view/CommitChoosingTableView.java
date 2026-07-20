package uet.fit.client.ui.view;

import javafx.scene.layout.VBox;
import uet.fit.client.ui.UIHelper;
import uet.fit.client.ui.controller.cia.CommitsChoosingTableController;

public class CommitChoosingTableView extends VBox {

	private final CommitsChoosingTableController controller;

	public CommitChoosingTableView() {
		controller = UIHelper.loadFXML(this, "/fxml/cia/CommitChoosingTable.fxml");
	}

	public CommitsChoosingTableController getController() {
		return controller;
	}
}
