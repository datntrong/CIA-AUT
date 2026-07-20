package uet.fit.client.ui.view;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import org.jetbrains.annotations.NotNull;
import uet.fit.client.ui.controller.test.TestCaseTabController;

import java.io.IOException;

public class TestCaseTab extends Tab {

	private final TestCaseTabController controller;

	private String testId;

	public TestCaseTab() {
		FXMLLoader loader = new FXMLLoader();
		loader.setRoot(this);

		try {
			loader.load(getClass().getResourceAsStream("/fxml/TestCaseTab.fxml"));
			controller = loader.getController();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void setTestId(@NotNull String testId) {
		this.testId = testId;
	}

	@NotNull
	public String getTestId() {
		return testId;
	}

	public TestCaseTabController getController() {
		return controller;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final TestCaseTab that = (TestCaseTab) o;

		return testId != null ? testId.equals(that.testId) : that.testId == null;
	}
}
