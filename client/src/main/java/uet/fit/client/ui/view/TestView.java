package uet.fit.client.ui.view;

import javafx.scene.control.SplitPane;
import uet.fit.client.ui.UIHelper;

public class TestView extends SplitPane {

	public TestView() {
		UIHelper.loadFXML(this);
	}
}
