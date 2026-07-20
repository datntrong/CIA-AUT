package uet.fit.client.ui.view;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import uet.fit.client.ui.controller.component.SourceCodeTabController;

import java.io.IOException;

public class SourceCodeTab extends Tab {

	private final SourceCodeTabController controller;

	public SourceCodeTab() {
		FXMLLoader loader = new FXMLLoader();
		loader.setRoot(this);

		try {
			loader.load(getClass().getResourceAsStream("/fxml/SourceCodeTab.fxml"));
			controller = loader.getController();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void setSource(String code) {
		controller.setSourceCode(code);
	}

	public String getSource() {
		return controller.getSourceCode();
	}

	public void highlight(int startLine, int endLine) {
		controller.highlight(startLine, endLine);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final SourceCodeTab that = (SourceCodeTab) o;

		return getText().equals(that.getText());
	}
}
