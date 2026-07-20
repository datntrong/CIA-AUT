package uet.fit.client.ui.controller.cia;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class SourceCompareController {
	@FXML private @NotNull Tab tab;
	@FXML private @NotNull SplitPane splitPane;
	@FXML private @NotNull VirtualizedScrollPane<?> scvOldVersion;
	@FXML private @NotNull VirtualizedScrollPane<?> scvNewVersion;
	@FXML private @NotNull SourceCodeViewController scvOldVersionController;
	@FXML private @NotNull SourceCodeViewController scvNewVersionController;

	private boolean singleMode = false;

	public static @NotNull SourceCompareController create() {
		try {
			final FXMLLoader loader = new FXMLLoader(SourceCompareController.class
					.getResource("/fxml/cia/SourceCompareTab.fxml"));
			loader.load();
			return loader.getController();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void configSingleMode(boolean old) {
		this.singleMode = true;
		splitPane.getItems().setAll(old ? List.of(scvOldVersion) : List.of(scvNewVersion));
	}

	public @NotNull Tab getTab() {
		return tab;
	}

	public void setTabTitle(@NotNull String text) {
		tab.setText(text);
	}

	public void setSourceCode(boolean old, @NotNull String sourceCode) {
		(old ? scvOldVersionController : scvNewVersionController).setSourceCode(sourceCode);
	}

	public void highlight(boolean old, int startLine, int endLine, boolean hideOther) {
		(old ? scvOldVersionController : scvNewVersionController).highlight(startLine, endLine);
		if (!singleMode) splitPane.setDividerPosition(0, hideOther ? old ? 1.0 : 0.0 : 0.5);
	}
}
