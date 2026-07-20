package uet.fit.client.ui.view;

import javafx.scene.layout.VBox;
import uet.fit.client.ui.UIHelper;
import uet.fit.client.ui.controller.env.ChooseVersionController;

public class ChooseVersionView extends VBox implements IEnvironmentBuilderStep {

	private final ChooseVersionController controller;

	public ChooseVersionView() {
		controller = UIHelper.loadFXML(this, "/fxml/env/ChooseVersion.fxml");
	}

	public ChooseVersionController getController() {
		return controller;
	}

	@Override
	public void clearState() {
		controller.clearState();
	}
}
