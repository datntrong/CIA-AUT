package uet.fit.client.ui.controller.component;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import uet.fit.client.common.User;
import uet.fit.client.thread.AUTThread;
import uet.fit.client.thread.task.DeleteUserCodeTask;
import uet.fit.client.thread.task.ModifyUserCodeTask;
import uet.fit.client.thread.task.UserCodeTestDataGenTask;
import uet.fit.client.ui.UIHelper;
import uet.fit.client.ui.controller.test.TestController;
import uet.fit.client.utils.FileUtils;
import uet.fit.dto.UserDTO.FunctionDTO;
import uet.fit.dto.UserDTO.UserTypedefRow;
import uet.fit.dto.logger.LogDTO;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserCodeTabController implements Initializable {
	@FXML
	private ComboBox<String> argumentBox;
	@FXML
	private CodeArea valueField;

	@FXML
	private Button btnAdd;
	@FXML
	private TableView<UserTypedefRow> userCodeTable;
	@FXML
	private TableColumn<UserTypedefRow, String> typeCol;
	@FXML
	private TableColumn<UserTypedefRow, String> varNameCol;
	@FXML
	private TableColumn<UserTypedefRow, String> valueCol;

	private FunctionDTO functionDTO;

	private List<String> argument = new ArrayList<>();
	private ObservableList<UserTypedefRow> dataList = FXCollections.observableArrayList();
	private ObservableList<UserTypedefRow> addingList = FXCollections.observableArrayList();
	private List<String> deletedListID = new ArrayList<>();
	private ObservableList<UserTypedefRow> modifiedList = FXCollections.observableArrayList();


	private static String codeText;

	private static final String[] KEYWORDS = new String[]{
			"asm", "else", "new", "this", "auto", "enum", "operator", "throw",
			"explicit", "private", "true", "break", "export", "protected",
			"try", "case", "extern", "public", "typedef", "catch", "false", "register",
			"typeid", "reinterpret_cast", "typename", "class", "for",
			"return", "union", "const", "friend", "const_cast", "goto",
			"using", "continue", "if", "sizeof", "virtual", "default", "inline", "include",
			"static", "delete", "static_cast", "volatile", "do", "struct",
			"wchar_t", "mutable", "switch", "while", "dynamic_cast", "namespace", "template"
	};

	private static final String[] PRIMITIVES = new String[]{
			"bool", "char", "float", "double", "void", "unsigned", "long", "short", "signed", "int"
	};

	private static final String KEYWORD_PATTERN = "\\b(?:" + String.join("|", KEYWORDS) + ")\\b";
	private static final String PRIMITIVE_PATTERN = "\\b(?:" + String.join("|", PRIMITIVES) + ")\\b";
	private static final String STRING_PATTERN = "(?<quote>[\"'])(?:\\\\.|(?:(?!\\k<quote>)[^\\r\\n\\\\])+)*\\k<quote>";
	private static final String COMMENT_PATTERN = "/\\*[^*]*\\*+(?:[^/][^*]*\\*+)*/|//.*";
	private static final String PAREN_PATTERN = "[()\\[\\]]+";
	private static final String BRACE_PATTERN = "[{}]+";
	private static final String SEMICOLON_PATTERN = "[-+/*;:.,!&<>^%~|=]+";
	private static final Pattern PATTERN = Pattern.compile(
			"(?<KEYWORD>" + KEYWORD_PATTERN + ")"
					+ "|(?<PRIMITIVE>" + PRIMITIVE_PATTERN + ")"
					+ "|(?<STRING>" + STRING_PATTERN + ")"
					+ "|(?<COMMENT>" + COMMENT_PATTERN + ")"
					+ "|(?<PAREN>" + PAREN_PATTERN + ")"
					+ "|(?<BRACE>" + BRACE_PATTERN + ")"
					+ "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")" );


	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {
		typeCol.setCellValueFactory(new PropertyValueFactory<>("myType"));
		varNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
		valueCol.setCellValueFactory(new PropertyValueFactory<>("value"));
		userCodeTable.setItems(dataList);
		valueField.setOnMouseClicked(e-> {
				valueField.setMaxHeight(250);
		});
		valueField.setOnMouseMoved(e-> {
			valueField.setMaxHeight(250);
		});

		valueField.setParagraphGraphicFactory(LineNumberFactory.get(valueField));

		valueField.multiPlainChanges()
				.successionEnds(Duration.ofMillis(100))
				.subscribe(ignore -> valueField.setStyleSpans(0, computeHighlighting(valueField.getText())));
		userCodeTable.setRowFactory(
				new Callback<TableView<UserTypedefRow>, TableRow<UserTypedefRow>>() {
					@Override
					public TableRow<UserTypedefRow> call(TableView<UserTypedefRow> tableView) {
						final TableRow<UserTypedefRow> row = new TableRow<>();
						final ContextMenu rowMenu = new ContextMenu();
						MenuItem editItem = new MenuItem("Edit");
						editItem.setOnAction (new EventHandler<ActionEvent>() {

							@Override
							public void handle(ActionEvent event) {
								try {
									Stage stage = new Stage();
									FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/userCodeWindow.fxml"));
									Parent root = null;
									root = loader.load();
									UserCodeWindowController controller = loader.getController();
									//List<UserTypedefRow> editList = new ArrayList<>(tableView.getSelectionModel().getSelectedItems());
									UserTypedefRow typedefRow = tableView.getSelectionModel().getSelectedItem();
									String userCode = typedefRow.getValue();
									controller.setUserTypedefRow(typedefRow);
									controller.setCodeAreaText(userCode);
									controller.setAdding(false);
									controller.setEditing(true);
									controller.setController(UserCodeTabController.this);
									Scene scene = new Scene(root);

									// set up the stage
									stage.setTitle("User Code Editor");
									stage.setWidth(600);
									stage.setHeight(400);
									stage.initModality(Modality.APPLICATION_MODAL);
									stage.setResizable(false);
									stage.setScene(scene);
									stage.show();
								} catch (IOException e) {
									e.getMessage();
								}
							}
						});;
						rowMenu.getItems().addAll(editItem);

						// only display context menu for non-empty rows:
						row.contextMenuProperty().bind(
								Bindings.when(row.emptyProperty())
										.then((ContextMenu) null)
										.otherwise(rowMenu));
						return row;
					}
				});
	}
	public void btnAdd_Clicked() {
		String argument = argumentBox.getValue();
		String[] compo = argument.split(" ");
		String value = valueField.getText();
		String id = String.valueOf(UUID.randomUUID());
		if(!value.trim().equals("")) {
			dataList.add(new UserTypedefRow(id, compo[0], compo[1], value));
			addingList.add(new UserTypedefRow(id, compo[0], compo[1], value));
		} else {
			Alert emptyAlert = UIHelper.showAlert(Alert.AlertType.CONFIRMATION, "CONFIRM",
					"The value field must not be empty.");
			emptyAlert.show();
		}
		//userCodeTable.setItems(dataList);
	}

	public ObservableList<UserTypedefRow> getUserInputList() {
		return dataList;
	}
	public void setDataList(List<UserTypedefRow> list) {
		for(UserTypedefRow utr : list) {
			dataList.add(utr);

		}
	}
	public void mouseHover() {
		valueField.setMaxHeight(50);
	}
	public void deleteBtn_Clicked() {
		List<UserTypedefRow> deleteList = new ArrayList<>(userCodeTable.getSelectionModel().getSelectedItems());
		Alert confirmAlert = UIHelper.showAlert(Alert.AlertType.CONFIRMATION, "CONFIRM",
				"Do you want to delete defines? ");
		Optional<ButtonType> option = confirmAlert.showAndWait();

		if (option.isPresent() && option.get() == ButtonType.OK) {
			for (UserTypedefRow utr : deleteList) {
				deletedListID.add(utr.getId());
				dataList.remove(utr);
			}

		}



	}


	public List<String> getDeletedListID() {
		return deletedListID;
	}

	public ObservableList<UserTypedefRow> getAddedList() {
		return addingList;
	}
	public void btnClear_Clicked() {

		Alert confirmAlert = UIHelper.showAlert(Alert.AlertType.CONFIRMATION, "CONFIRM",
				"Do you want to delete all defines? ");
		Optional<ButtonType> option = confirmAlert.showAndWait();

		if (option.isPresent() && option.get() == ButtonType.OK) {
			for (UserTypedefRow utr : dataList) {
				deletedListID.add(utr.getId());
			}
			dataList.clear();
			userCodeTable.getItems().clear();

		}
	}


	public void btnSubmit_Clicked() {
		if (!addingList.isEmpty()) {
			updateValue();
		}
		if(!deletedListID.isEmpty()) {
			deleteUserCode(deletedListID);
		}
		if(!modifiedList.isEmpty()) {
			modifyUserCode(modifiedList);
		}
	}

	public void setFunctionDTO(String env, String func, String file) {
		this.functionDTO = new FunctionDTO(env, file, func);
	}

	public void setArgumentList( List<String> argument) {
		this.argument = argument;
		argumentBox.getItems().addAll(argument);
	}



	public void updateValue() {
			String environment = functionDTO.getEnv();
			String file = functionDTO.getUut();
			String funcName = functionDTO.getSutName();
			UserCodeTestDataGenTask task = new UserCodeTestDataGenTask(environment, file, funcName, addingList, User.getInstance().getUsername());

			task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
				@Override
				public void handle(WorkerStateEvent workerStateEvent) {
					//String logContent = "Successfully changed value of " + node.getTitle() + "by user code";
					addingList.clear();
					String logContent = "Successfully changed value of arguments by user code";
					TestController.getInstance().logGeneral(LogDTO.TYPE_INF, logContent);
				}
			});
			task.setOnFailed(new EventHandler<WorkerStateEvent>() {
				@Override
				public void handle(WorkerStateEvent workerStateEvent) {
					String logContent = workerStateEvent.getSource().getException().getMessage();
					TestController.getInstance().logGeneral(LogDTO.TYPE_ERR, logContent);
				}
			});
			new AUTThread(task).start();

			//addingList.clear();
			//deletedListID.clear();
		}

	public void deleteUserCode(List<String> Ids) {
		String username = User.getInstance().getUsername();
		DeleteUserCodeTask task = new DeleteUserCodeTask(username, Ids);
		task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent workerStateEvent) {
				//String logContent = "Successfully changed value of " + node.getTitle() + "by user code";
				deletedListID.clear();
				String logContent = "Successfully delete usercode";
				TestController.getInstance().logGeneral(LogDTO.TYPE_INF, logContent);
			}
		});
		task.setOnFailed(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent workerStateEvent) {
				String logContent = workerStateEvent.getSource().getException().getMessage();
				TestController.getInstance().logGeneral(LogDTO.TYPE_ERR, logContent);
			}
		});
		new AUTThread(task).start();
	}

	public void modifyUserCode(ObservableList<UserTypedefRow> modifyList) {
		String username = User.getInstance().getUsername();
		ModifyUserCodeTask task = new ModifyUserCodeTask(modifyList, username);
		task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent workerStateEvent) {
				//String logContent = "Successfully changed value of " + node.getTitle() + "by user code";
				modifiedList.clear();
				String logContent = "Successfully modify usercode";
				TestController.getInstance().logGeneral(LogDTO.TYPE_INF, logContent);
			}
		});
		task.setOnFailed(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent workerStateEvent) {
				String logContent = workerStateEvent.getSource().getException().getMessage();
				TestController.getInstance().logGeneral(LogDTO.TYPE_ERR, logContent);
			}
		});
		new AUTThread(task).start();
	}

	public void browseBtn_Clicked() {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		FileChooser file = new FileChooser();
		file.setTitle("Open File");
		File file1 = file.showOpenDialog(alert.getOwner());
		alert.setTitle("Successful");
		alert.setContentText("Successfully open file " + file1.getName());
		//File file1 = file.
		//System.out.println(file.getInitialDirectory().getAbsolutePath());
		if(file1 != null) {
			String fileContent = FileUtils.read(file1.getAbsolutePath());
			this.getValueField().replaceText(fileContent);
		}
		alert.show();
	}

	public void editCodeBtn_Clicked() {
		try {
			Stage stage = new Stage();
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/userCodeWindow.fxml"));
			Parent root = null;
			root = loader.load();
			UserCodeWindowController controller = loader.getController();
			String userCode = this.getValueField().getText();
			//List<UserTypedefRow> editList = new ArrayList<>(tableView.getSelectionModel().getSelectedItems());
			controller.setCodeAreaText(userCode);
			controller.setEditing(false);
			controller.setAdding(true);
			controller.setController(UserCodeTabController.this);
			Scene scene = new Scene(root);

			// set up the stage
			stage.setTitle("User Code Editor");
			stage.setWidth(600);
			stage.setHeight(400);
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setResizable(false);
			stage.setScene(scene);
			stage.show();
		} catch (IOException e) {
			e.getMessage();
		}
	}




	/**
	 * Computing highlighting code syntax after edit code
	 *
	 * @param text code need to be computed
	 * @return all style for code area
	 */
	private static StyleSpans<Collection<String>> computeHighlighting(String text) {
		Matcher matcher = PATTERN.matcher(text);
		int lastPos = 0;
		StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
		while (matcher.find()) {
			String styleClass = matcher.group("KEYWORD") != null ? "keyword" :
					matcher.group("PRIMITIVE") != null ? "primitive" :
							matcher.group("STRING") != null ? "string" :
									matcher.group("COMMENT") != null ? "comment" :
											matcher.group("PAREN") != null ? "paren" :
													matcher.group("BRACE") != null ? "brace" :
															matcher.group("SEMICOLON") != null ? "semicolon" :
																	null; /* never happens */
			assert styleClass != null;
			spansBuilder.add(Collections.emptyList(), matcher.start() - lastPos);
			spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
			lastPos = matcher.end();
		}
		spansBuilder.add(Collections.emptyList(), text.length() - lastPos);
		return spansBuilder.create();
	}

	public CodeArea getValueField() {
		return valueField;
	}

	public ObservableList<UserTypedefRow> getModifiedList() {
		return modifiedList;
	}

	public void setModifiedList(ObservableList<UserTypedefRow> modifiedList) {
		this.modifiedList = modifiedList;
	}

	public ObservableList<UserTypedefRow> getDataList() {
		return dataList;
	}

	public void setDataList(ObservableList<UserTypedefRow> dataList) {
		this.dataList = dataList;
	}

	public ObservableList<UserTypedefRow> getAddingList() {
		return addingList;
	}

	public void setAddingList(ObservableList<UserTypedefRow> addingList) {
		this.addingList = addingList;
	}

	public void setDeletedListID(List<String> deletedListID) {
		this.deletedListID = deletedListID;
	}
}

