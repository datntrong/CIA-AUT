package uet.fit.client.ui;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.client.common.User;
import uet.fit.client.ui.controller.BaseController;
import uet.fit.client.ui.controller.HomeController;
import uet.fit.client.ui.controller.dialogs.AbstractLoginDialog;
import uet.fit.client.ui.controller.dialogs.LoginDialog;
import uet.fit.client.utils.ConfigLocation;
import uet.fit.client.utils.HttpUtils;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class AppStart extends Application {
	private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(AppStart.class);
	private static final @NotNull String SERVER_OPT = "-server=";

	private static @NotNull String @NotNull [] CMD_ARGS = new String[0];

	private static boolean parseArgsForServerUrl(@NotNull String @NotNull [] args) {
		for (final String arg : args) {
			if (arg.startsWith(SERVER_OPT)) {
				final String substring = arg.substring(SERVER_OPT.length());
				try {
					final String url = new URL(substring).toExternalForm();
					if (HttpUtils.testConnection(url)) {
						HttpUtils.setBaseUrl(url);
						try {
							Files.writeString(Path.of(ConfigLocation.SERVER_URL_FILE), url);
						} catch (final IOException exception) {
							LOGGER.info("Write server URL to file failed!", exception);
						}
						return true;
					}
				} catch (final IOException ignored) {
				}
			}
		}
		return false;
	}

	private static boolean readAndSetServerUrl() {
		try {
			final String string = Files.readString(Path.of(ConfigLocation.SERVER_URL_FILE));
			final String url = new URL(string).toExternalForm();
			if (HttpUtils.testConnection(url)) {
				HttpUtils.setBaseUrl(url);
				return true;
			}
		} catch (final IOException exception) {
			LOGGER.info("Read server URL from file failed!", exception);
		}
		return false;
	}

	private static boolean inputServerUrl() {
		final TextInputDialog dialog = new TextInputDialog("http://localhost:8080/");
		dialog.getDialogPane().getStylesheets().add("/css/style.css"); // Fix font display error on macos
		dialog.setHeaderText("Enter server URL below:");

		dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ANY, event -> {
			final String text = dialog.getEditor().getText();
			try {
				final String url = new URL(text).toExternalForm();
				if (HttpUtils.testConnection(url)) {
					HttpUtils.setBaseUrl(url);
				} else {
					showAlert(dialog, Alert.AlertType.WARNING, "Server connection error:",
							"Cannot connect to server.");
					event.consume();
				}
			} catch (final IOException e) {
				showAlert(dialog, Alert.AlertType.WARNING, "Server connection error:",
						"Input string is not a valid URL.");
				event.consume();
			}
		});
		return dialog.showAndWait().isPresent();
	}

	public static void showAlert(@NotNull TextInputDialog dialog, @NotNull Alert.AlertType type,
			@NotNull String header, @NotNull String content) {
		final Alert alert = new Alert(type);
		alert.getDialogPane().getStylesheets().add("/css/style.css"); // Fix font display error on macos
		alert.setTitle(type.toString());
		alert.setHeaderText(header);
		alert.setContentText(content);

		final DialogPane dialogPane = dialog.getDialogPane();
		dialogPane.setDisable(true);

		alert.showAndWait();

		dialogPane.setDisable(false);
	}

	public static void main(@NotNull String @NotNull [] args) {
		CMD_ARGS = args;
		launch(args);
	}

	@Override
	public void start(@NotNull Stage primaryStage) throws IOException, ExecutionException, InterruptedException {
		if (!parseArgsForServerUrl(CMD_ARGS) && !readAndSetServerUrl() && !inputServerUrl()) {
			throw new IOException("Cannot connect to server!");
		}
		LOGGER.info("Setup server url = " + HttpUtils.getBaseUrl());

		UIHelper.setPrimaryStage(primaryStage);

		primaryStage.setTitle("Automation Unit Testing Tool");
		Parent parent = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/Base.fxml")));
		Scene scene = new Scene(parent);

		primaryStage.setScene(scene);
		primaryStage.setResizable(true);
		primaryStage.show();

		logon();

		// close application
		primaryStage.setOnCloseRequest(event -> {
			try {
				HttpUtils.logoff(User.getInstance().getUsername());
			} catch (Exception exception) {
				exception.printStackTrace();
			}
			System.exit(0);
		});
	}

	public void logon() {
		while (true) {
			LoginDialog dialog = new LoginDialog();
			Optional<AbstractLoginDialog.Account> result = dialog.showAndWait();

			if (result.isPresent()) {
				String username = result.get().getUsername().trim();
				String password = result.get().getPassword().trim();
				Response response = HttpUtils.logon(username,password);

				if (response.getStatus() == 200) {
					BaseController.getInstance().displayUserName(username);
					User.getInstance().setUsername(username);
					User.getInstance().setPassword(password);
					UIHelper.showAlert(Alert.AlertType.INFORMATION, "SUCCESSFULLY",
									"Logon succeeded", response.readEntity(String.class))
							.showAndWait();
					User.getInstance().toJson();

					HomeController.getInstance().retrieveData();
					break;
				} else {
					String message;
					try {
						message = response.readEntity(String.class);
					} catch (Exception ex) {
						message = ex.getMessage();
					}
					UIHelper.showErrorAlert("Logon failed", message)
							.showAndWait();
				}
			} else {
				Alert alert = UIHelper.showAlert(Alert.AlertType.CONFIRMATION,
						"DO YOU WANT TO QUIT CIA-UT",
						"Click OK will quit your application",
						"Are you sure want to quit?");

				Optional<ButtonType> action = alert.showAndWait();

				if (action.isPresent() && action.get() == ButtonType.OK) {
					((Stage) alert.getOwner()).close();
					System.exit(0);
				}
			}
		}
	}
}
