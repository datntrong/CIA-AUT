package uet.fit.client.ui.view;

import javafx.scene.layout.VBox;
import uet.fit.client.ui.UIHelper;
import uet.fit.client.ui.controller.env.BrowseRepositoryController;
import uet.fit.client.ui.controller.env.ChooseVersionController;

public class BrowseRepositoryView extends VBox implements IEnvironmentBuilderStep {

	private final BrowseRepositoryController controller;

	public BrowseRepositoryView() {
		controller = UIHelper.loadFXML(this, "/fxml/env/BrowseRepository.fxml");
	}

	public BrowseRepositoryController getController() {
		return controller;
	}

	@Override
	public void clearState() {
		controller.clearState();
	}
}
