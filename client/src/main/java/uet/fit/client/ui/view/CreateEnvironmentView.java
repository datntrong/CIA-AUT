package uet.fit.client.ui.view;

import javafx.scene.layout.VBox;
import uet.fit.client.ui.UIHelper;
import uet.fit.client.ui.controller.env.CreateEnvironmentController;

import java.util.List;

public class CreateEnvironmentView extends VBox implements IEnvironmentBuilderStep {

	private final CreateEnvironmentController controller;

	public CreateEnvironmentView() {
		controller = UIHelper.loadFXML(this, "/fxml/env/CreateEnvironment.fxml");
	}

	@Override
	public void clearState() {
		controller.clearState();
	}

	public void setProFiles(List<String> files) {
		controller.setProFiles(files);
	}
}
