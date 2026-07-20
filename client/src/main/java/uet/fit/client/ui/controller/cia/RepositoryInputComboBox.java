package uet.fit.client.ui.controller.cia;

import javafx.beans.property.StringProperty;
import javafx.scene.control.ComboBox;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.cia.communicate.IdentifiedRequest;
import uet.fit.client.utils.CiaHttpUtils;

import java.util.Arrays;

import static uet.fit.client.ui.controller.cia.LogsViewController.logError;
import static uet.fit.client.ui.controller.cia.LogsViewController.logInfo;
import static uet.fit.client.ui.controller.cia.LogsViewController.logNormal;

public final class RepositoryInputComboBox extends ComboBox<String> {
	private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(RepositoryInputComboBox.class);

	private boolean initializing = false;
	private boolean initialized = false;

	public RepositoryInputComboBox() {
		setEditable(true);
		setOnMouseEntered(event -> {
			if (initializing || initialized) return;
			this.initializing = true;
			CiaHttpUtils.listRepositories(IdentifiedRequest.of()).whenCompleteAsync((responses, throwable) -> {
				this.initializing = false;
				if (responses != null) {
					logInfo("Success listing repositories!");
					Utils.enableAutoComplete(this, Arrays.asList(responses.getStrings()));
					this.initialized = true;
				} else {
					logError("Failed listing repositories!");
					if (throwable != null) {
						logNormal(throwable.getMessage());
						LOGGER.error("CiaHttpUtils.listRepositories throw!", throwable);
					}
				}
			});
		});
	}

	public @NotNull String getText() {
		return getEditor().getText();
	}

	public void setText(@NotNull String value) {
		getEditor().setText(value);
	}

	public void clear() {
		setText("");
		this.initialized = false;
	}

	public @NotNull StringProperty textProperty() {
		return getEditor().textProperty();
	}
}
