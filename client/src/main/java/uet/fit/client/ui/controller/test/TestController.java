package uet.fit.client.ui.controller.test;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.util.Callback;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.client.common.User;
import uet.fit.client.thread.AUTThread;
import uet.fit.client.thread.task.AutoGenTestDataTask;
import uet.fit.client.thread.task.DeleteTestCasesTask;
import uet.fit.client.thread.task.DuplicateTestCaseTask;
import uet.fit.client.thread.task.GenerateOverviewReportTask;
import uet.fit.client.thread.task.GetFucArgumentTask;
import uet.fit.client.thread.task.GetUserCodeTask;
import uet.fit.client.thread.task.InsertTestCaseTask;
import uet.fit.client.thread.task.InstrumentEnvTask;
import uet.fit.client.thread.task.ListTestCaseTask;
import uet.fit.client.thread.task.ViewCoverageTask;
import uet.fit.client.thread.task.RunTestTask;
import uet.fit.client.thread.task.ViewSourceTask;
import uet.fit.client.thread.task.ViewTestDataTask;
import uet.fit.client.ui.UIHelper;
import uet.fit.client.ui.controller.IPreviousStateCleanable;
import uet.fit.client.ui.view.CoverageView;
import uet.fit.client.ui.view.LogView;
import uet.fit.client.ui.view.ReportView;
import uet.fit.client.ui.view.SourceCodeTab;
import uet.fit.client.ui.view.TestCaseRow;
import uet.fit.client.ui.view.TestCaseTab;
import uet.fit.client.ui.view.TestStatusColumnCell;
import uet.fit.client.ui.view.UserCodeTab;
import uet.fit.client.utils.CalUtils;
import uet.fit.dto.ReportDTO;
import uet.fit.dto.UserDTO.UserTypedefRow;
import uet.fit.dto.env.EnvironmentDTO;
import uet.fit.dto.env.INavigableNode;
import uet.fit.dto.func.ViewSourceDTO;
import uet.fit.dto.logger.LogDTO;
import uet.fit.dto.test.AutoGenDTO;
import uet.fit.dto.test.DeletedTestEntry;
import uet.fit.dto.test.DeletedTestsDTO;
import uet.fit.dto.env.Source;
import uet.fit.dto.env.Subprogram;
import uet.fit.dto.func.FileLocation;
import uet.fit.dto.test.GenResultDTO;
import uet.fit.dto.test.TestListDTO;
import uet.fit.dto.test.SingleTestResultDTO;
import uet.fit.dto.test.TestResultDTO;
import uet.fit.dto.test.TestRow;
import uet.fit.dto.test.MultiTestResultDTO;
import uet.fit.dto.test.data.TestDataDTO;
import uet.fit.util.Utils;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestController implements Initializable, IPreviousStateCleanable {

	private static final Logger logger = LoggerFactory.getLogger(TestController.class);

	private static TestController instance;

	@FXML
	private MenuItem btnAutogenBasis;
	@FXML
	private MenuItem btnAutogenUserCode;
	@FXML
	private ScrollPane spToolbarContainer;
	@FXML
	private ToolBar toolbar;
	@FXML
	private Button btnCoverage;
	@FXML
	private Button btnRefresh;
	@FXML
	private LogView lgvGeneral;
	@FXML
	private LogView lgvBuild;
	@FXML
	private Tab tabTestCases;
	@FXML
	private Tab tabSources;
	@FXML
	private Tab tabReports;
	@FXML
	private Tab tabUserCode;
	@FXML
	private TabPane tpMain;
	@FXML
	private Button btnRun;
	@FXML
	private Button btnDelete;
	@FXML
	private Button btnDuplicate;
	@FXML
	private Button btnAdd;
	@FXML
	private MenuItem btnAutogenCFDS;
	@FXML
	private Button btnGenReport;
	@FXML
	private Button btnExport;
	@FXML
	private Button btnImport;
	@FXML
	private TreeView<INavigableNode> projectTree;
	@FXML
	private TableView<TestRow> testcaseTable;
	@FXML
	private TableColumn<TestRow, String> nameCol;
	@FXML
	private TableColumn<TestRow, String> statusCol;
	@FXML
	private TableColumn<TestRow, String> coverageCol;
	@FXML
	private TableColumn<TestRow, String> colAuthor;
	@FXML
	private TableColumn<TestRow, String> colTime;
	@FXML
	private TabPane tpTestCases;
	@FXML
	private TabPane tpSourceCodes;
	@FXML
	private TabPane tpReports;
	@FXML
	public TabPane tpUserCode;

	public static TestController getInstance() {
		return instance;
	}

	public TabPane getTpUserCode() {
		return tpUserCode;
	}

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {
		TestController.instance = this;

		minimizeToolbar();

		//viewUserCode();
//		SelectedUnitWrapper selectedUnit = new SelectedUnitWrapper();
//		String file = selectedUnit.file;
//		String function = selectedUnit.function;
//		String env = selectedUnit.environment;
//		tpUserCode.setOnMouseClicked(e-> {
//			viewUserCode(function,file,env);
//		});

		// refresh table view when list change
		// to set right color for cell
		testcaseTable.getItems().addListener(new ListChangeListener<TestRow>() {
			@Override
			public void onChanged(Change<? extends TestRow> change) {
				testcaseTable.refresh();
			}
		});

		nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
		statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
		statusCol.setCellFactory(new Callback<TableColumn<TestRow, String>, TableCell<TestRow, String>>() {
			@Override
			public TableCell<TestRow, String> call(TableColumn<TestRow, String> testRowStringTableColumn) {
				return new TestStatusColumnCell();
			}
		});
		coverageCol.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<TestRow, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(TableColumn.CellDataFeatures<TestRow, String> cellDataFeatures) {
				TestRow row = cellDataFeatures.getValue();
				SimpleStringProperty property = new SimpleStringProperty();
				float coverage = row.getCoverage();
				if (coverage == 1) {
					property.set("100%");
				} else if (coverage >= 0) {
					coverage *= 100;
					double roundCoverage = CalUtils.round(coverage, 2);
					property.set(roundCoverage + "%");
				}
				return property;
			}
		});
		colAuthor.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<TestRow, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(TableColumn.CellDataFeatures<TestRow, String> cellDataFeatures) {
				TestRow row = cellDataFeatures.getValue();
				SimpleStringProperty property = new SimpleStringProperty();
				String author = row.getOwner();
				String currentUser = User.getInstance().getUsername();
				if (author.equals(currentUser)) {
					property.set("You");
				} else {
					property.set(author);
				}
				return property;
			}
		});
		colTime.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<TestRow, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(TableColumn.CellDataFeatures<TestRow, String> cellDataFeatures) {
				TestRow row = cellDataFeatures.getValue();
				SimpleStringProperty property = new SimpleStringProperty();
				String createdTime = row.getCreatedTime();
				property.set(createdTime);
				return property;
			}
		});

		projectTree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		projectTree.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeItem<INavigableNode>>() {
			@Override
			public void changed(ObservableValue<? extends TreeItem<INavigableNode>> value, TreeItem<INavigableNode> oldItem, TreeItem<INavigableNode> newItem) {
				testcaseTable.getItems().clear();

				// select subprogram on project navigator
				if (newItem != null && newItem.getValue() instanceof Subprogram
						&& newItem.getParent() != null
						&& newItem.getParent().getValue() instanceof Source) {
					btnAdd.setDisable(false);
					btnRefresh.setDisable(false);

					String env = projectTree.getRoot().getValue().getTitle();
					String uut = newItem.getParent().getValue().getTitle();
					String sut = newItem.getValue().getTitle();
					getTestCases(env, uut, sut);
				} else {
					btnAdd.setDisable(true);
					btnRefresh.setDisable(true);
				}
			}
		});
		projectTree.setCellFactory(new Callback<TreeView<INavigableNode>, TreeCell<INavigableNode>>() {
			@Override

			public TreeCell<INavigableNode> call(TreeView<INavigableNode> stringTreeView) {
				TreeCell<INavigableNode> treeCell = new TreeCell<INavigableNode>() {
					@Override

					protected void updateItem(INavigableNode item, boolean empty) {
						super.updateItem(item, empty);

						if (!empty && item != null) {
							String title = item.getTitle();

							if (getTreeItem() instanceof ProjectTreeItem) {
								ProjectTreeItem treeItem = (ProjectTreeItem) getTreeItem();
								if (treeItem.isGeneratingProperty().get())
									title = "[Generating...]" + title;
							}

							setText(title);

							if (item instanceof EnvironmentDTO) {
								final ContextMenu contextMenu = new ContextMenu();
								MenuItem menuItem = new MenuItem("Re-instrument");
								menuItem.setOnAction(new EventHandler<ActionEvent>() {
									public void handle(ActionEvent e) {
										EnvironmentDTO dto = (EnvironmentDTO) item;
										String env = dto.getName();
										String user = User.getInstance().getUsername();
										String proPath = dto.getProFile();
										logger.debug("Env: " + env);
										logger.debug("ProPath: " + proPath);
										InstrumentEnvTask task = new InstrumentEnvTask(env, user, proPath);
										task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
											@Override
											public void handle(WorkerStateEvent workerStateEvent) {
												String logContent = "Successfully instrument";
												logGeneral(LogDTO.TYPE_INF, logContent);
											}
										});
										task.setOnFailed(new EventHandler<WorkerStateEvent>() {
											@Override
											public void handle(WorkerStateEvent workerStateEvent) {
												String logContent = task.getException().getMessage();
												logGeneral(LogDTO.TYPE_ERR, logContent);
											}
										});
										new AUTThread(task).start();
									}
								});
								contextMenu.getItems().addAll(menuItem);
								setContextMenu(contextMenu);
							} else if (item instanceof Subprogram) {
								final ContextMenu contextMenu = new ContextMenu();
								MenuItem menuItem = new MenuItem("User code");
								menuItem.setOnAction(new EventHandler<ActionEvent>() {
									public void handle(ActionEvent e) {
										SelectedUnitWrapper selectedUnit = new SelectedUnitWrapper();
										String file = selectedUnit.file;
										String function = selectedUnit.function;
										String env = selectedUnit.environment;
										viewUserCode(function,file,env);
									}
								});
								contextMenu.getItems().addAll(menuItem);
								setContextMenu(contextMenu);
							} else {
								setContextMenu(null);
							}

						} else {
							setText(null);
							setGraphic(null);

						}
					}
				};
				return treeCell;
			}

		});
		projectTree.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent mouseEvent) {
				if (mouseEvent.getClickCount() == 1) {
					if (mouseEvent.getButton() == MouseButton.SECONDARY) {

					}
				} else if (mouseEvent.getClickCount() == 2) {
					SelectedUnitWrapper selectedUnit = new SelectedUnitWrapper();
					String file = selectedUnit.file;
					String function = selectedUnit.function;
					String env = selectedUnit.environment;
//					tpUserCode.setOnMouseClicked(e-> {
					//viewUserCode(function,file,env);
//					});


					if (file != null) {
						ViewSourceTask task = new ViewSourceTask(file, function, env);
						task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
							@Override
							public void handle(WorkerStateEvent workerStateEvent) {
								String logContent = "Successfully get source of " + (function == null ? file : function);
								logGeneral(LogDTO.TYPE_INF, logContent);
								ViewSourceDTO dto = task.getValue();
								viewSource(dto);

							}
						});
						task.setOnFailed(new EventHandler<WorkerStateEvent>() {
							@Override
							public void handle(WorkerStateEvent workerStateEvent) {
								String logContent = task.getException().getMessage();
								logGeneral(LogDTO.TYPE_ERR, logContent);
							}
						});
						new AUTThread(task).start();
					}
				}
			}
		});

		testcaseTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		testcaseTable.setRowFactory(new Callback<TableView<TestRow>, TableRow<TestRow>>() {
			@Override
			public TableRow<TestRow> call(TableView<TestRow> testRowTableView) {
				return new TestCaseRow();
			}
		});
		testcaseTable.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent mouseEvent) {
				if (mouseEvent.getClickCount() == 2) {
					String user = User.getInstance().getUsername();
					TestRow testRow = testcaseTable.getSelectionModel().getSelectedItem();
					ViewTestDataTask task = new ViewTestDataTask(testRow.getId(), user);
					task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
						@Override
						public void handle(WorkerStateEvent workerStateEvent) {
							String logContent = "Successfully get test data of test case";
							logGeneral(LogDTO.TYPE_INF, logContent);
							viewTest(task.getValue());
						}
					});
					task.setOnFailed(new EventHandler<WorkerStateEvent>() {
						@Override
						public void handle(WorkerStateEvent workerStateEvent) {
							String logContent = task.getException().getMessage();
							logGeneral(LogDTO.TYPE_ERR, logContent);
						}
					});
					new AUTThread(task).start();
				}
			}
		});

		final BooleanBinding isNonSelectedTestProperty = testcaseTable.getSelectionModel()
				.selectedItemProperty()
				.isNull();
		btnDelete.disableProperty().bind(isNonSelectedTestProperty);
		btnDuplicate.disableProperty().bind(isNonSelectedTestProperty);
		btnRun.disableProperty().bind(isNonSelectedTestProperty);
		btnCoverage.disableProperty().bind(isNonSelectedTestProperty);
		//viewUserCode();
	}

	private void minimizeToolbar() {
		// wrap all buttons in a pane and add tooltip
		// to support view tooltip when disabled buttons
		List<Node> toolbarChildren = new ArrayList<>(toolbar.getItems());
		toolbar.getItems().clear();
		for (Node node : toolbarChildren) {
			if (node instanceof Button) {
				Button button = (Button) node;
//				final String text = button.getText();
//				button.setText(null);
				Pane pane = new Pane(button);
//				pane.hoverProperty().addListener(new ChangeListener<Boolean>() {
//					@Override
//					public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean t1) {
//						if (t1) {
//							button.setText(text);
//						} else {
//							button.setText(null);
//						}
//					}
//				});
				Tooltip tooltip = button.getTooltip();
				Tooltip.install(pane, tooltip);
				toolbar.getItems().add(pane);
			} else {
				toolbar.getItems().add(node);
			}
		}

		// scale toolbar width to fit container if width > 280
		spToolbarContainer.widthProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
				double newWidth = t1.doubleValue();
				spToolbarContainer.setFitToWidth(newWidth > 280);
			}
		});
	}

	private void getTestCases(String env, String uut, String sut) {
		ListTestCaseTask task = new ListTestCaseTask(uut, sut, env);
		task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent workerStateEvent) {
				String logContent = "Successfully get all test case of " + sut;
				logGeneral(LogDTO.TYPE_INF, logContent);
				TestListDTO testListDTO = task.getValue();
				if (isSelect(uut, sut))
					testcaseTable.getItems().setAll(testListDTO.getList());
			}
		});
		task.setOnFailed(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent workerStateEvent) {
				String logContent = task.getException().getMessage();
				logGeneral(LogDTO.TYPE_ERR, logContent);
			}
		});
		new AUTThread(task).start();
	}

	public void projectTree_Clicked(MouseEvent actionEvent) {
	}

	public void refreshButtonClicked() {
		SelectedUnitWrapper selectedUnit = new SelectedUnitWrapper();
		if (selectedUnit.isSelectFunction) {
			testcaseTable.getItems().clear();
			String env = selectedUnit.environment;
			String uut = selectedUnit.file;
			String sut = selectedUnit.function;
			getTestCases(env, uut, sut);
		}
	}

	public void addButton_Clicked(ActionEvent actionEvent) {
		SelectedUnitWrapper selectedUnit = new SelectedUnitWrapper();

		if (selectedUnit.isSelectFunction) {
			String environment = selectedUnit.environment;
			String funcName = selectedUnit.function;
			String filePath = selectedUnit.file;

			UIHelper.showTextInputDialog("Add Test", "Please input test case name", "Name")
					.showAndWait()
					.ifPresent(new Consumer<String>() {
						@Override
						public void accept(String name) {
							String owner = User.getInstance().getUsername();
							InsertTestCaseTask task = new InsertTestCaseTask(owner, environment, name, filePath, funcName);
							task.setOnSucceeded(workerStateEvent -> {
								String logContent = "Successfully inserted test case " + name;
								logGeneral(LogDTO.TYPE_INF, logContent);
								TestDataDTO dataDTO = task.getValue();
								if (isSelect(filePath, funcName))
									insertNewTestcase(owner, dataDTO);
								viewTest(dataDTO);
							});
							task.setOnFailed(new EventHandler<WorkerStateEvent>() {
								@Override
								public void handle(WorkerStateEvent workerStateEvent) {
									String logContent = task.getException().getMessage();
									logGeneral(LogDTO.TYPE_ERR, logContent);
								}
							});
							new AUTThread(task).start();
						}
					});
		} else {
			UIHelper.showErrorAlert("Cannot insert test case for this type of node (Source File)")
					.showAndWait();
		}
	}

	public void showProjectTree(EnvironmentDTO root) {
		String repository = root.getGitUrl();
		String commit = Utils.shortenCommitHash(root.getCommit());
		logGeneral(LogDTO.TYPE_INF, String.format("Testing repository %s - commit %s", repository, commit));

		TreeItem<INavigableNode> treeRoot = new ProjectTreeItem(root);
		dfs(treeRoot);
		projectTree.setRoot(treeRoot);
	}

	class ProjectTreeItem extends TreeItem<INavigableNode> {

		public ProjectTreeItem(INavigableNode iNavigableNode) {
			super(iNavigableNode);
			isGenerating.addListener(new ChangeListener<Boolean>() {
				@Override
				public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean t1) {
					projectTree.refresh();
				}
			});
		}

		private final BooleanProperty isGenerating = new SimpleBooleanProperty(false);

		public BooleanProperty isGeneratingProperty() {
			return isGenerating;
		}
	}

	@FXML
	public void viewCoverage_Clicked() {
		List<TestRow> selectedRows = new ArrayList<>(testcaseTable.getSelectionModel().getSelectedItems());
		List<String> testIds = new ArrayList<>();
		for (TestRow testRow : selectedRows) {
			testIds.add(testRow.getId());
		}

		ViewCoverageTask task = new ViewCoverageTask(User.getInstance().getUsername(), testIds);
		task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent workerStateEvent) {
				String logContent = "Successfully compute coverage of " + selectedRows.size() + " test case";
				logGeneral(LogDTO.TYPE_INF, logContent);
				MultiTestResultDTO dto = task.getValue();
				updateExecution(dto);
				openCoverageView(dto.getReport());
			}
		});

		task.setOnFailed(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent workerStateEvent) {
				String logContent = task.getException().getMessage();
				logGeneral(LogDTO.TYPE_ERR, logContent);
			}
		});

		new AUTThread(task).start();
	}

	public void genReportButton_Clicked(ActionEvent event) {

		btnGenReport.setDisable(true);

		List<TreeItem<INavigableNode>> selectedTreeItems = new ArrayList<>(projectTree.getSelectionModel().getSelectedItems());

		List<Source> uutList = new ArrayList<>();
		for (TreeItem<INavigableNode> treeItem : selectedTreeItems) {
			if (treeItem.getValue() instanceof Source) {
				Source source = (Source) treeItem.getValue();
				uutList.add(source);
			}
		}

		String username = User.getInstance().getUsername();
		String env = "";

		if (projectTree.getRoot() != null) {
			env = projectTree.getRoot().getValue().getTitle();
		}

		GenerateOverviewReportTask task = new GenerateOverviewReportTask(username, env, uutList);
		task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent workerStateEvent) {
				openReportView(task.getValue());
				btnGenReport.setDisable(false);
			}
		});
		task.setOnFailed(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent workerStateEvent) {
				String logContent = task.getException().getMessage();
				logGeneral(LogDTO.TYPE_ERR, logContent);
				UIHelper.showErrorAlert("Cannot generate overview report for this environment!", logContent)
						.showAndWait();
				btnGenReport.setDisable(false);
			}
		});
		new AUTThread(task).start();
	}

	private void viewSource(ViewSourceDTO sourceWrapper) {
		String file = sourceWrapper.getName();
		String content = sourceWrapper.getContent();

		SourceCodeTab tab = findSourceTab(file);

		if (tab == null) {
			tab = new SourceCodeTab();
			tab.setText(file);

			tab.setSource(content);

			tpSourceCodes.getTabs().add(tab);
		} else if (!tab.getSource().equals(content)) {
			tab.setSource(content);
		}

		FileLocation location = sourceWrapper.getFileLocation();
		if (location != null) {
			int startLine = location.getStartLine();
			int endLine = location.getEndLine();

			tab.highlight(startLine, endLine);
		}

		// select new tab
		tpSourceCodes.getSelectionModel().select(tab);
		tpMain.getSelectionModel().select(tabSources);
	}

	public void run_Clicked() {
		final String username = User.getInstance().getUsername();

		List<TestRow> selectedRows = new ArrayList<>(testcaseTable.getSelectionModel().getSelectedItems());
		selectedRows.removeIf(Objects::isNull);
		Iterator<TestRow> iterator = selectedRows.iterator();

		runNextTest(username, iterator);
	}

	private void runNextTest(final String username, Iterator<TestRow> iterator) {
		if (iterator.hasNext()) {
			TestRow selectedRow = iterator.next();
			String id = selectedRow.getId();
			String name = selectedRow.getName();

			RunTestTask task = new RunTestTask(username, id);
			task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
				@Override
				public void handle(WorkerStateEvent workerStateEvent) {
					String logContent = "Successfully run test case " + name;
					logGeneral(LogDTO.TYPE_INF, logContent);
					SingleTestResultDTO dto = task.getValue();
					updateExecution(dto);
					openCoverageView(dto.getReport());

					runNextTest(username, iterator);
				}
			});
			task.setOnFailed(new EventHandler<WorkerStateEvent>() {
				@Override
				public void handle(WorkerStateEvent workerStateEvent) {
					String logContent = task.getException().getMessage();
					logGeneral(LogDTO.TYPE_ERR, logContent);

					runNextTest(username, iterator);
				}
			});

			new AUTThread(task).start();
		}
	}

	public void autogenCFDS_Clicked() {
		autogen_Clicked(AutoGenDTO.CFDS, btnAutogenCFDS);
	}

	public void autogenUserCode_Clicked() {
		autogen_Clicked(AutoGenDTO.USER_CODE, btnAutogenUserCode);
	}

	public void autogenBasisPath_Clicked() {
		autogen_Clicked(AutoGenDTO.BASIS_PATH, btnAutogenBasis);
	}

	private Iterator<ProjectTreeItem> findSelectedTreeItems() {
		List<TreeItem<INavigableNode>> selectedItems = new ArrayList<>(projectTree.getSelectionModel().getSelectedItems());
		return selectedItems.stream().filter(i -> i instanceof ProjectTreeItem).map(i -> (ProjectTreeItem) i).iterator();
	}

	private void autogen_Clicked(String strategy, MenuItem btnAutogen) {
		btnAutogen.setDisable(true);

		String user = User.getInstance().getUsername();

		SelectedUnitWrapper selectedUnit = new SelectedUnitWrapper();
		String env = selectedUnit.environment;

		if (selectedUnit.isSelectFunction) {
			String uut = selectedUnit.file;
			String sut = selectedUnit.function;
			ProjectTreeItem treeItem;
			if (projectTree.getSelectionModel().getSelectedItem() instanceof ProjectTreeItem)
				treeItem = (ProjectTreeItem) projectTree.getSelectionModel().getSelectedItem();
			else
				treeItem = null;
			if (treeItem != null) treeItem.isGeneratingProperty().set(true);

			AutoGenTestDataTask task = new AutoGenTestDataTask(user, uut, sut, env, strategy);
			task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
				@Override
				public void handle(WorkerStateEvent workerStateEvent) {
					String logContent = "Completed generate test data for " + sut;
					logGeneral(LogDTO.TYPE_INF, logContent);
					GenResultDTO resultDTO = task.getValue();

					if (sut != null && uut != null && isSelect(uut, sut))
						testcaseTable.getItems().addAll(resultDTO.getTests());

					if (!strategy.equals(AutoGenDTO.BASIS_PATH))
						openCoverageView(resultDTO.getReport());
					btnAutogen.setDisable(false);
					if (treeItem != null) treeItem.isGeneratingProperty().set(false);
				}
			});
			task.setOnFailed(new EventHandler<WorkerStateEvent>() {
				@Override
				public void handle(WorkerStateEvent workerStateEvent) {
					String logContent = task.getException().getMessage();
					logGeneral(LogDTO.TYPE_ERR, logContent);
					UIHelper.showErrorAlert("Cannot generate test data!", logContent)
							.showAndWait();
					btnAutogen.setDisable(false);
					if (treeItem != null) treeItem.isGeneratingProperty().set(false);
				}
			});
			new AUTThread(task).start();
		} else {
			Iterator<String> units;
			Iterator<ProjectTreeItem> selectedItems;
			if (selectedUnit.isSelectUnit) {
				units = Collections.singleton(selectedUnit.file).iterator();
				selectedItems = Stream.of(projectTree.getSelectionModel().getSelectedItem())
						.filter(n -> n instanceof ProjectTreeItem)
						.map(n -> (ProjectTreeItem) n)
						.iterator();
			} else {
				units = projectTree.getRoot().getChildren()
						.stream()
						.map(n -> n.getValue().getTitle())
						.iterator();
				selectedItems = projectTree.getRoot().getChildren()
						.stream()
						.filter(n -> n instanceof ProjectTreeItem)
						.map(n -> (ProjectTreeItem) n)
						.iterator();
			}

			generateNextTests(user, env, strategy, units, selectedItems, btnAutogen);
		}
	}

	private void generateNextTests(@NotNull String user, @NotNull String env,
			@NotNull String strategy, @NotNull Iterator<String> unitIter, Iterator<ProjectTreeItem> treeItemIterator, MenuItem btnAutogen) {

		if (!unitIter.hasNext() || !treeItemIterator.hasNext()) {
			btnAutogen.setDisable(false);
			return;
		}

		String uut = unitIter.next();
		ProjectTreeItem treeItem = treeItemIterator.next();
		treeItem.isGeneratingProperty().set(true);


		AutoGenTestDataTask task = new AutoGenTestDataTask(user, uut, null, env, strategy);
		task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent workerStateEvent) {
				String logContent = "Completed generate test data for " + uut;
				logGeneral(LogDTO.TYPE_INF, logContent);
				GenResultDTO resultDTO = task.getValue();
				if (!strategy.equals(AutoGenDTO.BASIS_PATH))
					openCoverageView(resultDTO.getReport());
				treeItem.isGeneratingProperty().set(false);
				generateNextTests(user, env, strategy, unitIter, treeItemIterator, btnAutogen);
			}
		});
		task.setOnFailed(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent workerStateEvent) {
				String logContent = task.getException().getMessage();
				logGeneral(LogDTO.TYPE_ERR, logContent);
				treeItem.isGeneratingProperty().set(false);
				generateNextTests(user, env, strategy, unitIter, treeItemIterator, btnAutogen);
			}
		});

		new AUTThread(task).start();
	}

	private void openCoverageView(@NotNull ReportDTO report) {
		String title = report.getTitle();
		String content = report.getContent();

		tpReports.getTabs().removeIf(t -> t.getText().equals(title));

		Tab tab = new Tab(title);
		CoverageView coverageView = new CoverageView();
		coverageView.setContent(content);
		tab.setContent(coverageView);

		tpReports.getTabs().add(tab);
		tpReports.getSelectionModel().select(tab);
		tpMain.getSelectionModel().select(tabReports);
	}

	private void openReportView(@NotNull ReportDTO report) {
		String title = report.getTitle();
		String content = report.getContent();

		tpReports.getTabs().removeIf(t -> t.getText().equals(title));

		Tab tab = new Tab(title);
		ReportView reportView = new ReportView();
		reportView.setContent(content);
		tab.setContent(reportView);

		tpReports.getTabs().add(tab);
		tpReports.getSelectionModel().select(tab);
		tpMain.getSelectionModel().select(tabReports);
	}

//	private void updateExecution(@NotNull SingleTestResultDTO testResult) {
//		List<TestRow> table = new ArrayList<>(testcaseTable.getItems());
//		for (TestRow row : table) {
//			if (testResult.getId().equals(row.getId())) {
//				row.setCoverage(testResult.getCoverage());
//				String status = String.format("%d/%d", testResult.getPass(), testResult.getTotal());
//				row.setStatus(status);
//				testcaseTable.refresh();
//				break;
//			}
//		}
//	}

	private void updateExecution(@NotNull TestResultDTO testResult) {
		List<TestRow> table = new ArrayList<>(testcaseTable.getItems());
		for (TestResultDTO.Entry result : testResult.getTests()) {
			for (TestRow row : table) {
				if (result.getId().equals(row.getId())) {
					row.setCoverage(result.getCoverage());
					row.setStatus(result.getStatus());
					testcaseTable.refresh();
					break;
				}
			}
		}
	}

	public void duplicate_Clicked() {
		SelectedUnitWrapper selectedUnit = new SelectedUnitWrapper();
		TestRow selectedRow = testcaseTable.getSelectionModel().getSelectedItem();
		if (selectedUnit.isSelectFunction && selectedRow != null) {
			String funcName = selectedUnit.function;
			String filePath = selectedUnit.file;
			String id = selectedRow.getId();
			String originName = selectedRow.getName();
			String environment = selectedUnit.environment;

			UIHelper.showTextInputDialog("Duplicate " + originName, "Please input test case name", "Name")
					.showAndWait()
					.ifPresent(new Consumer<String>() {
						@Override
						public void accept(String name) {
							String owner = User.getInstance().getUsername();
							DuplicateTestCaseTask task = new DuplicateTestCaseTask(owner, id, name, environment, filePath, funcName);
							task.setOnSucceeded(workerStateEvent -> {
//								logGeneral(LogDTO.TYPE_INF, logContent);
								TestDataDTO dataDTO = task.getValue();
								if (isSelect(filePath, funcName))
									insertNewTestcase(owner, dataDTO);
								viewTest(dataDTO);
							});
							task.setOnFailed(workerStateEvent -> {
								String logContent = task.getException().getMessage();
								logGeneral(LogDTO.TYPE_ERR, logContent);
							});
							new AUTThread(task).start();
						}
					});
		} else {
			UIHelper.showErrorAlert("Cannot insert test case for this type of node (Source File)")
					.showAndWait();
		}
	}

	public void export_Clicked() {
		List<TestRow> selectedRows = new ArrayList<>(testcaseTable.getSelectionModel().getSelectedItems());
		if(selectedRows.size() > 0) {
			SelectedUnitWrapper selectedUnit = new SelectedUnitWrapper();
			String funcName = selectedUnit.function;
			String filePath = selectedUnit.file;
			UIHelper.showChooseTestTable(filePath, funcName, selectedRows);
		}
		else {
			String env = projectTree.getRoot().getValue().getTitle();
			List<TreeItem<INavigableNode>> uutList = projectTree.getRoot().getChildren();

			UIHelper.showChooseTestTable(env, uutList);
		}
	}

	public void import_Clicked() {
		UIHelper.importTestCase(projectTree.getRoot().getValue().getTitle());
	}

	public void deleteButton_Clicked() {
		List<String> testIds = new ArrayList<>();
		List<TestRow> selectedRows = new ArrayList<>(testcaseTable.getSelectionModel().getSelectedItems());

		StringBuilder content = new StringBuilder();
		for (TestRow testRow : selectedRows) {
			testIds.add(testRow.getId());
			content.append(testRow.getName());
			if (selectedRows.indexOf(testRow) != selectedRows.size() - 1)
				content.append(", ");
		}

		Alert confirmAlert = UIHelper.showAlert(Alert.AlertType.CONFIRMATION, "CONFIRM",
				"Do you want to delete test cases? ", content.toString());
		Optional<ButtonType> option = confirmAlert.showAndWait();

		if (option.isPresent() && option.get() == ButtonType.OK) {
			DeleteTestCasesTask task = new DeleteTestCasesTask(User.getInstance().getUsername(), testIds);

			task.setOnSucceeded(workerStatEvent -> {
				DeletedTestsDTO deletedTests = task.getValue();
				int existCount = 0;
				int deletedCount = 0;
				for (DeletedTestEntry test : deletedTests.getList()) {
					if (test.getStatus() != DeletedTestEntry.Status.SUCCESS) {
						existCount++;
					} else {
						deletedCount++;
						testcaseTable.getItems().removeIf(i -> i.getId().equals(test.getId()));
						closeTest(test.getId());
					}
				}

				if (deletedCount > 0) {
					String logContent = "Successfully deleted " + deletedCount + " test case(s)";
					logGeneral(LogDTO.TYPE_INF, logContent);
				}

				if (existCount == 0) {
					UIHelper.showAlert(Alert.AlertType.INFORMATION, "SUCCESS", "Successfully deleted!")
							.showAndWait();
				} else {
					UIHelper.showStatusDelete(deletedTests.getList(), selectedRows);
				}
			});
			task.setOnFailed(workerStateEvent -> {
				String logContent = task.getException().getMessage();
				logGeneral(LogDTO.TYPE_ERR, logContent);
				UIHelper.showErrorAlert("Deleted failed.")
						.showAndWait();
			});
			new AUTThread(task).start();
		}
	}

	public void testcase_Clicked(MouseEvent mouseEvent) {

	}

	public void insertNewTestcase(String owner, String id, String name) {
		final String defaultStatus = "N/A";
		final float defaultCoverage = -1;
		TestRow testRow = new TestRow(id, name, defaultStatus, defaultCoverage, owner, LocalDateTime.now().toString());
		testcaseTable.getItems().add(testRow);
	}


	public void insertNewTestcase(String owner, TestDataDTO dataDTO) {
		final String id = dataDTO.getId();
		final String testName = dataDTO.getTestCaseName();
		insertNewTestcase(owner, id, testName);
	}

	public void viewTest(@NotNull TestDataDTO testData) {
		// remove old tab
		closeTest(testData.getId());

		// create new tab
		TestCaseTab tab = new TestCaseTab();
		tab.setTestId(testData.getId());
		tab.setText(testData.getTestCaseName());
		tab.getController().loadTestCase(testData);

		// add and select new tab
		tpTestCases.getTabs().add(tab);
		tpTestCases.getSelectionModel().select(tab);
		tpMain.getSelectionModel().select(tabTestCases);
	}

	private void closeTest(@NotNull String id) {
		tpTestCases.getTabs().removeIf(tab -> tab instanceof TestCaseTab
				&& ((TestCaseTab) tab).getTestId().equals(id));
	}

	private SourceCodeTab findSourceTab(@NotNull String file) {
		return (SourceCodeTab) tpSourceCodes.getTabs().stream()
				.filter(tab -> tab instanceof SourceCodeTab && tab.getText().equals(file))
				.findFirst()
				.orElse(null);
	}

	private void dfs(TreeItem<INavigableNode> treeItem) {
		INavigableNode newNode = treeItem.getValue();
		if (newNode.getChildren() != null) {
			for (INavigableNode child : newNode.getChildren()) {
				TreeItem<INavigableNode> childNode = new ProjectTreeItem(child);
				treeItem.getChildren().add(childNode);
				dfs(childNode);
			}
		}
	}

	@Override
	public void clearPreviousState() {
		testcaseTable.getItems().clear();
		projectTree.setRoot(null);
		tpTestCases.getTabs().clear();
		tpSourceCodes.getTabs().clear();
		tpReports.getTabs().clear();
		tpUserCode.getTabs().clear();
	}

	public void logGeneral(byte type, String message) {
		lgvGeneral.log(message, type);
	}

	public void logBuild(byte type, String message) {
		lgvBuild.log(message, type);
	}

	public boolean isSelect(@NotNull String file, @Nullable String func) {
		SelectedUnitWrapper selectedUnit = new SelectedUnitWrapper();
		return selectedUnit.equals(file, func);
	}

	private void viewUserCode(String funcName, String file, String env) {
		boolean ifExist = false;
		for(Tab tab : tpUserCode.getTabs()) {
			if(tab.getText().equals(funcName + "/" +file + "/" + env)) {
				ifExist = true;
			}
		}
		if(!ifExist) {
			UserCodeTab tab = new UserCodeTab(funcName, env, file);
			getUserCode(file, funcName, env,tab);
			tpUserCode.getTabs().add(tab);
			tpUserCode.getSelectionModel().select(tab);
			tpMain.getSelectionModel().select(tabUserCode);
			if (file != null) {
				GetFucArgumentTask task = new GetFucArgumentTask(env, funcName, file);
				task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
					@Override
					public void handle(WorkerStateEvent workerStateEvent) {
						String logContent = "Successfully get all argument of  " + (funcName == null ? file : funcName);
						logGeneral(LogDTO.TYPE_INF, logContent);
						List<String> list = task.getValue();
						tab.getController().setArgumentList(list);
						tab.getController().setFunctionDTO(env, funcName, file);
					}
				});
				task.setOnFailed(new EventHandler<WorkerStateEvent>() {
					@Override
					public void handle(WorkerStateEvent workerStateEvent) {
						String logContent = task.getException().getMessage();
						logGeneral(LogDTO.TYPE_ERR, logContent);
					}
				});
				new AUTThread(task).start();
			}
		}

	}

	private void getUserCode(String uut, String sut, String env, UserCodeTab tab) {
		GetUserCodeTask task = new GetUserCodeTask(uut, sut, env);
		task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent workerStateEvent) {
				String logContent = "Successfully get all usercode of  " + (sut == null ? uut : sut);
				logGeneral(LogDTO.TYPE_INF, logContent);
				List<UserTypedefRow> rows = task.getValue();
//				for(UserTypedefRow row : rows) {
//					System.out.println(row.getValue());
//					System.out.println(row.getId());
//					System.out.println(row.getName());
//				}
				tab.getController().setDataList(rows);
			}
		});
		task.setOnFailed(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent workerStateEvent) {
				String logContent = task.getException().getMessage();
				logGeneral(LogDTO.TYPE_ERR, logContent);
			}
		});
		new AUTThread(task).start();
	}



	private class SelectedUnitWrapper {

		String environment;
		String file;
		String function;

		boolean isSelectFunction = false;
		boolean isSelectUnit = false;

		SelectedUnitWrapper() {
			if (projectTree.getRoot() != null && projectTree.getRoot().getValue() != null) {
				environment = projectTree.getRoot().getValue().getTitle();
			}

			TreeItem<INavigableNode> selectedItem = projectTree.getSelectionModel().getSelectedItem();
			if (selectedItem != null) {
				if (selectedItem.getValue() instanceof Subprogram) {
					function = selectedItem.getValue().getTitle();

					if (selectedItem.getParent() != null
							&& selectedItem.getParent().getValue() instanceof Source) {
						file = selectedItem.getParent().getValue().getTitle();
					}

					isSelectFunction = true;
				} else if (selectedItem.getValue() instanceof Source) {
					file = selectedItem.getValue().getTitle();
					function = null;
					isSelectUnit = true;
				}
			}
		}

		boolean equals(@NotNull String file, @Nullable String function) {
			if (this.file.equals(file)) {
				if (this.function == null && function == null) {
					return true;
				} else if (this.function != null && function != null) {
					return this.function.equals(function);
				}
			}
			return false;
		}
	}
}
