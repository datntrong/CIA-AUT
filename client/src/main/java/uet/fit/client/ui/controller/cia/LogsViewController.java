package uet.fit.client.ui.controller.cia;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class LogsViewController {
	public static @NotNull LogsViewController create() {
		try {
			final FXMLLoader loader = new FXMLLoader(LogsViewController.class
					.getResource("/fxml/cia/LogsView.fxml"));
			loader.load();
			return loader.getController();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@FXML private @NotNull SplitPane content;
	@FXML private @NotNull Tab tabClientLogs;
	@FXML private @NotNull Tab tabServerLogs;
	@FXML private @NotNull ListView<LogMessage> lvClientLogs;
	@FXML private @NotNull TextArea taServerLogs;

	public LogsViewController() {
	}

	@FXML
	private void initialize() {
		tabClientLogs.setOnCloseRequest(event -> {
			event.consume();
			CIAMainViewController.getInstance().hideLog();
		});
		tabServerLogs.setOnCloseRequest(event -> {
			event.consume();
			hideServerLog();
		});
		lvClientLogs.setFocusTraversable(true);
		lvClientLogs.setCellFactory(StyleClassListCell::new);
		lvClientLogs.getFocusModel().focusedItemProperty().addListener((observable, old, logMessage) -> {
			if (logMessage instanceof ServerMessage) {
				showServerMessage((ServerMessage) logMessage);
			} else {
				hideServerLog();
			}
		});
	}

	private void refreshClientMessage(@NotNull ServerMessage serverMessage) {
		final int index = lvClientLogs.getItems().indexOf(serverMessage);
		if (index >= 0) lvClientLogs.getItems().set(index, serverMessage);
	}

	private void refreshServerMessage(@NotNull ServerMessage serverMessage) {
		if (content.getDividers().get(0).getPosition() <= 0.99
				&& lvClientLogs.getFocusModel().getFocusedItem() == serverMessage) {
			taServerLogs.setText(serverMessage.getLog());
			taServerLogs.setWrapText(false);
			taServerLogs.setScrollTop(Double.MAX_VALUE);
		}
	}

	private void showServerMessage(@NotNull ServerMessage serverMessage) {
		if (content.getDividers().get(0).getPosition() >= 0.99) {
			content.setDividerPosition(0, 0.5);
		}
		lvClientLogs.getFocusModel().focus(lvClientLogs.getItems().indexOf(serverMessage));
		taServerLogs.setText(serverMessage.getLog());
		taServerLogs.setWrapText(false);
		taServerLogs.setScrollTop(Double.MAX_VALUE);
	}

	private void hideServerLog() {
		content.setDividerPosition(0, 1.0);
		taServerLogs.clear();
	}

	void clear() {
		lvClientLogs.getItems().clear();
		taServerLogs.clear();
	}

	private void logMessage(@NotNull LogMessage message) {
		Platform.runLater(() -> {
			final ObservableList<LogMessage> items = lvClientLogs.getItems();
			items.add(message);
			lvClientLogs.scrollTo(items.indexOf(message));
			CIAMainViewController.getInstance().showLog();
		});
	}

	private void logClient(@NotNull String message, @NotNull List<@NotNull String> styles) {
		logMessage(new LogMessage(message, styles));
	}

	private @NotNull ServerMessage logServer(@NotNull String message, @NotNull List<@NotNull String> styles) {
		final ServerMessage serverMessage = new ServerMessage(message, styles);
		logMessage(serverMessage);
		return serverMessage;
	}


	static class LogMessage {
		protected @NotNull String text;
		protected @NotNull List<@NotNull String> styles;

		LogMessage(@NotNull String text, @NotNull List<@NotNull String> styles) {
			this.text = text;
			this.styles = styles;
		}

		final @NotNull String getText() {
			return text;
		}

		final @NotNull List<@NotNull String> getStyles() {
			return styles;
		}
	}

	public static final class ServerMessage extends LogMessage {
		public static final @NotNull List<@NotNull String> API_RUNNING = List.of("log", "api-running");
		public static final @NotNull List<@NotNull String> API_SUCCESS = List.of("log", "api-success");
		public static final @NotNull List<@NotNull String> API_FAILED = List.of("log", "api-error");

		private final @NotNull StringBuffer buffer = new StringBuffer();

		ServerMessage(@NotNull String text, @NotNull List<@NotNull String> styles) {
			super(text, styles);
		}

		void appendLog(@NotNull CharSequence sequence) {
			buffer.append(sequence);
		}

		@NotNull String getLog() {
			return buffer.toString();
		}

		void setLogMessage(@NotNull String text, @NotNull List<@NotNull String> styles) {
			this.text = text;
			this.styles = styles;
		}
	}

	private static final class StyleClassListCell extends ListCell<LogMessage> {
		public StyleClassListCell(@NotNull ListView<LogMessage> view) {
			setWrapText(true);
		}

		@Override
		protected void updateItem(@Nullable LogMessage item, boolean empty) {
			super.updateItem(item, empty);
			final ObservableList<String> styleClass = getStyleClass();
			if (empty || item == null) {
				setText(null);
				styleClass.clear();
			} else {
				setText(item.getText());
				styleClass.setAll(item.getStyles());
			}
		}
	}


	private static final @NotNull List<@NotNull String> LOG_NORMAL = List.of("log", "log-normal");
	private static final @NotNull List<@NotNull String> LOG_INFO = List.of("log", "log-info");
	private static final @NotNull List<@NotNull String> LOG_ERROR = List.of("log", "log-error");

	public static void logNormal(@NotNull String message) {
		CIAMainViewController.getInstance().logger().logClient(message, LOG_NORMAL);
	}

	public static void logInfo(@NotNull String message) {
		CIAMainViewController.getInstance().logger().logClient(message, LOG_INFO);
	}

	public static void logError(@NotNull String message) {
		CIAMainViewController.getInstance().logger().logClient(message, LOG_ERROR);
	}

	public static @NotNull ServerMessage logApiCall(@NotNull String message) {
		return CIAMainViewController.getInstance().logger().logServer(message, ServerMessage.API_RUNNING);
	}

	public static void updateClientMessage(@NotNull ServerMessage serverMessage,
			@NotNull String text, @NotNull List<@NotNull String> styles) {
		serverMessage.setLogMessage(text, styles);
		Platform.runLater(() -> CIAMainViewController.getInstance().logger().refreshClientMessage(serverMessage));
	}

	public static void updateServerMessage(@NotNull ServerMessage serverMessage, @NotNull String message) {
		serverMessage.appendLog(message);
		Platform.runLater(() -> CIAMainViewController.getInstance().logger().refreshServerMessage(serverMessage));
	}
}
