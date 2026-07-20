package uet.fit.client.ui.view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.client.common.User;
import uet.fit.client.thread.AUTThread;
import uet.fit.client.thread.task.AssertTestDataTask;
import uet.fit.client.ui.controller.test.TestCaseTabController;
import uet.fit.client.ui.controller.test.TestController;
import uet.fit.client.utils.TestTreeUtils;
import uet.fit.dto.logger.LogDTO;
import uet.fit.dto.test.data.HaveTypeTestNode;
import uet.fit.dto.test.data.ITestNode;

import java.io.File;
import java.util.Arrays;

/**
 * Represents a single row/column in the test case tab
 */
public class AssertColumnCell extends TreeTableCell<ITestNode, String> {

	private static final Logger logger = LoggerFactory.getLogger(AssertColumnCell.class);

	private static final String USER_CODE = "USER CODE";
	private static final String RETURN_TAG = File.separator + "return";

	@Override
	public void startEdit() {
		logger.debug("Start editing on the cell at line " + this.getIndex());
		super.startEdit();

		showComboBox();
	}

	protected void showComboBox() {
		ITestNode dataNode = getTreeTableRow().getTreeItem().getValue();

		setText(null);
		setGraphic(null);

		if (isVisible(dataNode)) {
			HaveTypeTestNode valueNode = (HaveTypeTestNode) dataNode;
			logger.debug("The type of data node corresponding to the cell: " + valueNode.getClass());
			logger.debug("Support assert method:");

			ComboBox<String> comboBox = new ComboBox<>();
			ObservableList<String> options = FXCollections.observableArrayList();

			HaveTypeTestNode expectedNode = TestTreeUtils.getExpectedValue(valueNode);
			if (expectedNode == null)
				expectedNode = valueNode;
			String prevMethod = expectedNode.getAssertMethod();
			if (prevMethod == null)
				comboBox.setValue("<<Select Method>>");
			else
				comboBox.setValue(prevMethod);

			String[] supportedMethods = expectedNode.getSupportAsserts();
			for (String supportedMethod : supportedMethods) {
				// TODO: disable user code
				if (!supportedMethod.equals(USER_CODE))
					options.add(supportedMethod);
			}

			comboBox.setItems(options);
			// Chỉnh sửa cho combobox vừa với ô của tree table.
			comboBox.setMaxWidth(getTableColumn().getMaxWidth());
			// Khi chọn giá trị trong combobox thì commit giá trị đó.
			comboBox.valueProperty().addListener((ov, oldValue, newValue) -> commitEdit(newValue));

			setGraphic(comboBox);

		}
	}

	@Override
	public void cancelEdit() {
		super.cancelEdit();
		getTreeTableView().refresh();
		logger.debug("Canceled the edit on the cell");
	}

	@Override
	public void commitEdit(String newValue) {
		super.commitEdit(newValue);

		if (newValue.isEmpty())
			newValue = null;

		TreeTableRow<ITestNode> row = getTreeTableRow();
		ITestNode dataNode = row.getItem();

		if (dataNode == null) {
			logger.debug("There is matching between a cell and its data");
			return;
		}

		try {
			if (dataNode instanceof HaveTypeTestNode) {
				HaveTypeTestNode valueNode = (HaveTypeTestNode) dataNode;
				String testcaseId = TestTreeUtils.getTestCaseId(valueNode);
				String path = valueNode.getPath();

				AssertTestDataTask task = new AssertTestDataTask(testcaseId, path, newValue, User.getInstance().getUsername());
				task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
					@Override
					public void handle(WorkerStateEvent workerStateEvent) {
						String logContent = "Successfully asserted " + valueNode.getTitle();
						TestController.getInstance().logGeneral(LogDTO.TYPE_INF, logContent);
						TreeTableView<ITestNode> treeTable = getTreeTableView();
						TestCaseTabController.loadTreeTable(treeTable, task.getValue());
					}
				});
				task.setOnFailed(new EventHandler<WorkerStateEvent>() {
					@Override
					public void handle(WorkerStateEvent workerStateEvent) {
						Throwable ex = task.getException();
						String message = String.format("Failed to assert %s: %s", valueNode.getTitle(), ex.getMessage());
						TestController.getInstance().logGeneral(LogDTO.TYPE_ERR, message);
					}
				});
				new AUTThread(task).start();

//				HaveTypeTestNode expectedNode = TestTreeUtils.getExpectedValue(valueNode);
//				if (expectedNode != null) {
//					expectedNode.setAssertMethod(newValue);
//				}
//				valueNode.setAssertMethod(newValue);

				// TODO: show user code dialog
//				if (AssertMethod.USER_CODE.equals(newValue)) {
//					ParameterUserCodeDialogController controller = ParameterUserCodeDialogController
//							.getAssertInstance(valueNode);
//					if (controller != null && controller.getStage() != null) {
//						controller.setTestCase(testCase);
//						controller.showAndWaitStage(UIController.getPrimaryStage());
//					}
//				}
			}

		} catch (Exception ex) {
			logger.debug("Error " + ex.getMessage() + " when entering data for " + dataNode.getClass());
			ex.printStackTrace();
		}

//		getTreeTableView().refresh();
//		logger.debug("Refreshed the current test case tab");

//		// save data tree to the test script
//		TestCaseManager.exportBasicTestCaseToFile(testCase);
	}

	@Override
	public void updateItem(String item, boolean empty) {
		super.updateItem(item, empty);

		TreeItem<ITestNode> treeItem = getTreeTableRow().getTreeItem();

		if (treeItem != null && treeItem.getValue() instanceof HaveTypeTestNode && isVisible(treeItem.getValue())) {
			HaveTypeTestNode valueNode = (HaveTypeTestNode) treeItem.getValue();
			String assertMethod = valueNode.getAssertMethod();
			HaveTypeTestNode expectedNode = TestTreeUtils.getExpectedValue(valueNode);
			if (expectedNode != null) {
				String expectedAssertMethod = expectedNode.getAssertMethod();
				if (assertMethod == null) {
					assertMethod = expectedAssertMethod;
				} else if (!assertMethod.equals(expectedAssertMethod))
					assertMethod = expectedAssertMethod;

				expectedNode.setAssertMethod(assertMethod);
				valueNode.setAssertMethod(assertMethod);

//                    if (AssertMethod.USER_CODE.equals(assertMethod)) {
//                        AssertUserCode userCode = valueNode.getAssertUserCode();
//                        expectedNode.setAssertUserCode(userCode);
//                    }
			}

			if (assertMethod != null) {
				setText(assertMethod);

				//TODO display user code assert
//				if (assertMethod.equals(AssertMethod.USER_CODE)) {
//					Button button = new Button();
//					ImageView imageView = new ImageView(
//							new Image(getClass().getResourceAsStream("/icons/file/newEnvironment.png")));
//					button.setGraphic(imageView);
//					imageView.setOnMousePressed(new EventHandler<MouseEvent>() {
//						@Override
//						public void handle(MouseEvent event) {
//							System.out.println("CLICK");
//						}
//					});
//					button.setOnAction(new EventHandler<ActionEvent>() {
//						@Override
//						public void handle(ActionEvent event) {
//							commitEdit(AssertMethod.USER_CODE);
//						}
//					});
//					setGraphic(button);
//
//				} else
//					setGraphic(null);

			} else {
				setText("<<Select Method>>");
				setGraphic(null);
			}

		} else {
			setText(null);
			setGraphic(null);
		}

	}

	private boolean isVisible(ITestNode dataNode) {
		if (dataNode == null)
			return false;

		if (!(dataNode instanceof HaveTypeTestNode))
			return false;

		HaveTypeTestNode valueNode = (HaveTypeTestNode) dataNode;

		if (valueNode.getSupportAsserts() == null)
			return false;

		boolean emptySupportedMethods = Arrays.stream(valueNode.getSupportAsserts())
				.noneMatch(m -> m != null && !m.isBlank()
						&& !m.equals(USER_CODE)
				);

		if (emptySupportedMethods)
			return false;

		boolean isReturnItem = valueNode.getPath().contains(RETURN_TAG);

		return !isReturnItem || TestTreeUtils.isExpected(dataNode);
	}
}
