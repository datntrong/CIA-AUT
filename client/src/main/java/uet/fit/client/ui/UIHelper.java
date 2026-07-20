package uet.fit.client.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Callback;
import uet.fit.client.ui.controller.dialogs.ChooseProPathController;
import uet.fit.client.ui.controller.dialogs.ExportTestCaseController;
import uet.fit.client.ui.controller.dialogs.ImportTestCaseController;
import uet.fit.client.ui.controller.dialogs.StatusDeleteController;
import uet.fit.client.utils.CalUtils;
import uet.fit.dto.env.INavigableNode;
import uet.fit.dto.test.DeletedTestEntry;
import uet.fit.dto.test.TestRow;

import java.io.IOException;
import java.util.List;

public class UIHelper {

	private static Stage primaryStage;

	public static Stage getPrimaryStage() {
		return primaryStage;
	}

	public static void setPrimaryStage(Stage primaryStage) {
		UIHelper.primaryStage = primaryStage;
	}

	public static Alert showAlert(Alert.AlertType type, String title, String header, String content) {
		Alert alert = new Alert(type);
		// Fix font display error on macos
		DialogPane dialogPane = alert.getDialogPane();
		dialogPane.getStylesheets().add("/css/style.css");
		alert.setTitle(title);
		alert.setHeaderText(header);
		alert.setContentText(content);
		alert.initOwner(primaryStage);
		return alert;
	}

	public static Alert showErrorAlert(String header, String content) {
		return showAlert(Alert.AlertType.ERROR, "ERROR", header, content);
	}

	public static Alert showErrorAlert(String header) {
		return showErrorAlert(header, "");
	}

	public static Alert showAlert(Alert.AlertType type, String title, String header) {
		return showAlert(type, title, header, "");
	}

	public static TextInputDialog showTextInputDialog(String title, String header, String content) {
		TextInputDialog textInputDialog = new TextInputDialog();
		// Fix font display error on macos
		DialogPane dialogPane = textInputDialog.getDialogPane();
		dialogPane.getStylesheets().add("/css/style.css");
		textInputDialog.setTitle(title);
		textInputDialog.setHeaderText(header);
		textInputDialog.setContentText(content);
		Node btnOk = textInputDialog.getDialogPane().lookupButton(ButtonType.OK);
		TextField tfInput = textInputDialog.getEditor();
		tfInput.textProperty().addListener((observableValue, oldName, name) -> {
			if (name == null || CalUtils.validateName(name.trim())) {
				tfInput.setStyle("");
				btnOk.setDisable(false);
			} else {
				tfInput.setStyle("-fx-border-color: red");
				btnOk.setDisable(true);
			}
		});
		textInputDialog.initOwner(primaryStage);
		return textInputDialog;
	}

	public static <T extends Parent, C> C loadFXML(T component) {
		String controllerName = component.getClass().getSimpleName();
		String fileName = "/fxml/" + controllerName.substring(0, controllerName.length() - 4) + ".fxml";
		return loadFXML(component, fileName);
	}

	public static <T extends Parent, C> C loadFXML(T component, String path) {
		FXMLLoader loader = new FXMLLoader();
		loader.setRoot(component);

		try {
			loader.load(component.getClass().getResourceAsStream(path));
			component.getStylesheets().add("/css/componentStyle.css");
			return loader.getController();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void showChooseProPath(List<String> proPaths, Callback<String, Void> callback) {
		FXMLLoader loader = new FXMLLoader(UIHelper.class.getResource("/fxml/dialogs/ChooseProPath.fxml"));
		try {
			AnchorPane chooseProPath = loader.load();
			ChooseProPathController chooseProPathController = loader.getController();
			chooseProPathController.setLvProPath(proPaths);
			chooseProPathController.setCallback(callback);

			Scene scene = new Scene(chooseProPath);
			Stage stage = new Stage();
			stage.setTitle("Choose pro path");
			stage.setScene(scene);

			stage.showAndWait();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void showStatusDelete(List<DeletedTestEntry> deletedTestEntries, List<TestRow> testRowList) {
		FXMLLoader loader = new FXMLLoader(UIHelper.class.getResource("/fxml/dialogs/StatusDelete.fxml"));
		try {
			AnchorPane statusDelete = loader.load();
			StatusDeleteController statusDeleteController = loader.getController();
			statusDeleteController.showStatus(deletedTestEntries, testRowList);

			Scene scene = new Scene(statusDelete);
			Stage stage = new Stage();
			stage.setTitle("Status Delete");
			stage.setScene(scene);
			stage.initOwner(primaryStage);
			stage.show();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void showChooseTestTable(String env, List<TreeItem<INavigableNode>> uutList) {
		FXMLLoader loader = new FXMLLoader(UIHelper.class.getResource("/fxml/dialogs/ExportTestCase.fxml"));
		try {
			AnchorPane chooseTest = loader.load();
			ExportTestCaseController exportTestCaseController = loader.getController();
			exportTestCaseController.loadAllTests(env, uutList);

			Scene scene = new Scene(chooseTest);
			scene.getStylesheets().add("/css/componentStyle.css");
			Stage stage = new Stage();
			stage.setTitle("Export test case");
			stage.setScene(scene);
			stage.initOwner(primaryStage);
			stage.show();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void showChooseTestTable(String uut, String sut, List<TestRow> testRows) {
		FXMLLoader loader = new FXMLLoader(UIHelper.class.getResource("/fxml/dialogs/ExportTestCase.fxml"));
		try {
			AnchorPane chooseTest = loader.load();
			ExportTestCaseController exportTestCaseController = loader.getController();
			exportTestCaseController.exportSelectedTest(uut, sut, testRows);

			Scene scene = new Scene(chooseTest);
			scene.getStylesheets().add("/css/componentStyle.css");
			Stage stage = new Stage();
			stage.setTitle("Export test case");
			stage.setScene(scene);
			stage.initOwner(primaryStage);
			stage.show();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void importTestCase(String env) {
		FXMLLoader loader = new FXMLLoader(UIHelper.class.getResource("/fxml/dialogs/ImportTestCase.fxml"));
		try {
			AnchorPane importTest = loader.load();
			ImportTestCaseController importTestCaseController = loader.getController();
			importTestCaseController.setEnv(env);

			Scene scene = new Scene(importTest);
			scene.getStylesheets().add("/css/componentStyle.css");
			Stage stage = new Stage();
			stage.setTitle("Import test case");
			stage.setScene(scene);
			stage.initOwner(primaryStage);
			stage.show();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
