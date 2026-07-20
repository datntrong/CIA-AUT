package uet.fit.client.ui.view;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTablePosition;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.client.common.User;
import uet.fit.client.thread.AUTThread;
import uet.fit.client.thread.task.EnterTestDataTask;
import uet.fit.client.ui.controller.test.TestCaseTabController;
import uet.fit.client.ui.controller.test.TestController;
import uet.fit.client.utils.TestTreeUtils;
import uet.fit.dto.logger.LogDTO;
import uet.fit.dto.test.data.EditableTestNode;
import uet.fit.dto.test.data.HaveTypeTestNode;
import uet.fit.dto.test.data.HaveValueTestNode;
import uet.fit.dto.test.data.ITestNode;
import uet.fit.dto.test.data.LabelTestNode;
import uet.fit.dto.test.data.SutTestNode;
import uet.fit.dto.test.data.UnitTestNode;

public abstract class AbstractTableCell extends TreeTableCell<ITestNode, String> {

    public static final String OPTION_VOID_POINTER_PRIMITIVE_TYPES = "Primitive Types";
    public static final String OPTION_VOID_POINTER_STRUCTURE_TYPES = "Structure Types";
    public static final String OPTION_VOID_POINTER_USER_CODE = "User Code";

    protected final static Logger logger = LoggerFactory.getLogger(AbstractTableCell.class);

    protected TextField textField;

    // improve UX when entering
    protected boolean escapePressed = false;
    protected TreeTablePosition<ITestNode, ?> tablePos;

    /**
     * Add boundary tooltip to current cell
     */
    private void addBoundaryTooltip() {
        // Get datanode object
        TreeTableRow<ITestNode> row = getTreeTableRow();

        if (row.getItem() instanceof HaveTypeTestNode) {
            HaveTypeTestNode dataNode = (HaveTypeTestNode) row.getItem();

            // TODO: get boundary
            // Get the boundaries
//            String type = dataNode.getType();
//            type = VariableTypeUtils.removeRedundantKeyword(type);
//            PrimitiveBound bound = BoundaryManager.getInstance().getUsingBoundOfDataTypes().getBounds().get(type);
//            String lbound = bound.getLower();
//            String ubound = bound.getUpper();
//
//            // Set the boundaries on the tooltip
//            if (textField != null)
//                textField.setTooltip(new Tooltip("[" + lbound + ".." + ubound + "]"));
        }
    }

    protected void update(ITestNode unit) {
        // first "if" to display "USER CODE" for parameters that use user code
        if (unit instanceof HaveTypeTestNode && ((HaveTypeTestNode) unit).isUseUserCode()) {
            setEditable(true);
            setText("<<User code>>");

        } else if (unit instanceof EditableTestNode) {
            // int
            setEditable(true);
            setText(((EditableTestNode) unit).getValue());

        } else if (unit instanceof HaveValueTestNode) {
            // char
            setEditable(false);
            setText(((HaveValueTestNode) unit).getValue());

        } else {
            setText(null);
            setGraphic(null);
        }
    }

    protected void showText(CellType type) {
        ITestNode dataNode = getTreeTableRow().getTreeItem().getValue();

        if (dataNode != null) {
            logger.debug("The type of data node corresponding to the cell: " + dataNode.getClass());

            // if it is a return variable node then ignore
            boolean isExpected = TestTreeUtils.isExpected(dataNode);
            if ((isExpected && type == CellType.INPUT)) {
//                disable();
                return;
            }

            // Các node cần nhập vào text field
            if (dataNode instanceof EditableTestNode) {
                setText(null);

                EditableTestNode editableTestNode = (EditableTestNode) dataNode;
                if (editableTestNode.isMultipleChoices()) {
                    // Các node cần có combo-box
                    setGraphic(createComboBox(editableTestNode));
                } else {
                    setGraphic(textField);
                    textField.setText(getValueForTextField(dataNode));
                    textField.selectAll();
                    textField.requestFocus();

                    textField.addEventFilter(MouseEvent.MOUSE_ENTERED, event -> {
                        addBoundaryTooltip();
                    });
                }

            }

            // TODO: user code
//            else if (dataNode instanceof UnresolvedDataNode && testCase instanceof TestCase) {
//                UnresolvedDataNode unresolvedDataNode = (UnresolvedDataNode) dataNode;
//                ParameterUserCodeDialogController controller = ParameterUserCodeDialogController.getInstance(unresolvedDataNode);
//                if (controller != null && controller.getStage() != null) {
//                    controller.setTestCase((TestCase) testCase);
//                    controller.showAndWaitStage(UIController.getPrimaryStage());
//                }
//            }

            else {
                logger.debug("Do not support to enter data for " + dataNode.getClass());
            }
        } else {
            logger.debug("There is no matching between a cell and a data node");
        }
    }

    private String getValueForTextField(ITestNode dataNode) {
        if (dataNode instanceof HaveValueTestNode) {
            return ((HaveValueTestNode) dataNode).getValue();
        }

        return null;
    }

    protected void saveValueWhenUsersPressEnter() {
        logger.debug("Set event when users click enter on the cell");
        if (textField == null) {
            textField = new TextField();
            textField.setOnKeyPressed(t -> {
                if (t.getCode() == KeyCode.ESCAPE) {
                    escapePressed = true;
                }
            });

            textField.setOnKeyReleased((KeyEvent t) -> {
                if (t.getCode() == KeyCode.ENTER) {
                    commitEdit(textField.getText());
                }
            });
        }
    }

    protected ComboBox<String> createComboBox(EditableTestNode dataNode) {
        ComboBox<String> comboBox = new ComboBox<>();
        ObservableList<String> options = FXCollections.observableArrayList(dataNode.getChoices());
        comboBox.setValue(dataNode.getValue());
        comboBox.setEditable(dataNode.getCategory().equals("EnumDataNode"));
        comboBox.setItems(options);
        // Chỉnh sửa cho combobox vừa với ô của tree table.
        comboBox.setMaxWidth(getTableColumn().getMaxWidth());
        // Khi chọn giá trị trong combobox thì commit giá trị đó.
        comboBox.valueProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String oldValue, String newValue) {
                doCommitEdit(newValue);
                logger.debug(newValue);
            }
        });
        return comboBox;
    }

    public ContextMenu setupContextMenu(ITestNode dataNode) {
        ContextMenu contextMenu = new ContextMenu();

//        if (testCase instanceof TestPrototype)
//            return contextMenu;

        // TODO: user code
//        if (dataNode instanceof HaveTypeTestNode) {
//            MenuItem miUserCode = new MenuItem("User Code");
//            miUserCode.setOnAction(event -> {
//                ParameterUserCodeDialogController controller = ParameterUserCodeDialogController
//                        .getInstance((ValueDataNode) dataNode);
//                if (controller != null && controller.getStage() != null) {
//                    controller.setTestCase((TestCase) testCase);
//                    controller.showAndWaitStage(UIController.getPrimaryStage());
//                    inputCellHandler.update(this, dataNode);
//                    TestCaseManager.exportBasicTestCaseToFile((TestCase) testCase);
//
//                    if (getTreeTableRow().getTreeItem() instanceof TestDataTreeItem) {
//                        TestDataTreeItem treeItem = (TestDataTreeItem) getTreeTableRow().getTreeItem();
//                        // reload các con của tree item
//                        if (!(dataNode instanceof NormalCharacterDataNode || dataNode instanceof NormalNumberDataNode
//                                || dataNode instanceof EnumDataNode))
//                            TestCaseTreeTableController.loadChildren(testCase, treeItem);
//                    }
//                }
//            });
//
//            contextMenu.getItems().add(miUserCode);
//        }

        return contextMenu;
    }

    @Override
    public void cancelEdit() {
        if (escapePressed) {
            // this is a cancel event after escape key
            super.cancelEdit();
            logger.debug("ESCAPE to canceled the edit on the cell");
        } else {
            // this is not a cancel event after escape key
            // we interpret it as commit.
            ITestNode dataNode = getTreeTableRow().getItem();
            if (dataNode instanceof EditableTestNode) {
                EditableTestNode editableTestNode = (EditableTestNode) dataNode;
                // get the new text from the view
                String newText = textField.getText();
                if (newText != null && !newText.isEmpty() && !newText.equals(editableTestNode.getValue()))
                // commit the new text to the model
                commitEdit(newText);
            }
        }
        getTreeTableView().refresh();
    }

    @Override
    public void commitEdit(String newValue) {
//        if (!isEditing()) return;

        final TreeTableView<ITestNode> treeTable = getTreeTableView();
        if (treeTable != null) {
            // Inform the TableView of the edit being ready to be committed.
            TreeTableColumn.CellEditEvent editEvent = new TreeTableColumn.CellEditEvent(
                    treeTable,
                    tablePos,
                    TreeTableColumn.editCommitEvent(),
                    newValue
            );

            Event.fireEvent(getTableColumn(), editEvent);

            super.cancelEdit(); // cannel before do commit to avoid doing commitEdit twice

            doCommitEdit(newValue);
        }
    }

    protected abstract void doCommitEdit(String newValue);

    protected void clearValue(ITestNode node) {
        if (node instanceof EditableTestNode) {
            String testcaseId = TestTreeUtils.getTestCaseId(node);
            String path = node.getPath();

            EnterTestDataTask task = new EnterTestDataTask(testcaseId, path, null, User.getInstance().getUsername());
            task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                @Override
                public void handle(WorkerStateEvent workerStateEvent) {
                    String logContent = "Successfully cleared value of " + node.getTitle();
                    TestController.getInstance().logGeneral(LogDTO.TYPE_INF, logContent);
                    TreeTableView<ITestNode> treeTable = getTreeTableView();
                    TestCaseTabController.loadTreeTable(treeTable, task.getValue());
                }
            });
            task.setOnFailed(new EventHandler<WorkerStateEvent>() {
                @Override
                public void handle(WorkerStateEvent workerStateEvent) {
                    Throwable ex = task.getException();
                    String message = String.format("Failed to clear value of %s: %s", node.getTitle(), ex.getMessage());
                    TestController.getInstance().logGeneral(LogDTO.TYPE_ERR, message);
                }
            });
            new AUTThread(task).start();
        }
    }

    @Override
    public void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
    }

    protected void disable() {
        setDisable(true);
        setStyle("-fx-text-fill: #808080");
    }

    protected static boolean isMultipleValue(String newValue) {
        return newValue.trim().matches("\\{.+\\}");
    }

//    protected static String[] preprocessValue(String newValue) {
//        int start = newValue.indexOf(SpecialCharacter.OPEN_BRACE) + 1;
//        int end = newValue.lastIndexOf(SpecialCharacter.CLOSE_BRACE);
//        String regex = ",(?![^()]*\\))(?=(?:[^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)";
//        String[] values = newValue.substring(start, end).split(regex, -1);
//        for (int i = 0; i < values.length; i++) {
//            values[i] = values[i].trim();
//        }
//        return values;
//    }

    protected void onRetrieveValue(EditableTestNode node, String newValue) {
        String testcaseId = TestTreeUtils.getTestCaseId(node);
        String path = node.getPath();

        EnterTestDataTask task = new EnterTestDataTask(testcaseId, path, newValue, User.getInstance().getUsername());
        task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                String logContent = "Successfully changed value of " + node.getTitle();
                TestController.getInstance().logGeneral(LogDTO.TYPE_INF, logContent);
                TreeTableView<ITestNode> treeTable = getTreeTableView();
                TestCaseTabController.loadTreeTable(treeTable, task.getValue());
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

//        if (isMultipleValue(newValue)) {
//            if (valueDataNode.isStubArgument()) {
//                ValueDataNode primaryNode = valueDataNode.getIterators().get(0).getDataNode();
//                primaryNode.getIterators().removeIf(i -> i.getDataNode() != primaryNode);
//                String[] values = preprocessValue(newValue);
//                for (int i = 0; i < values.length; i++) {
//                    if (i != 0) {
//                        Iterator iterator = new Iterator(primaryNode.clone());
//                        iterator.setStartIdx(i + 1);
//                        iterator.setRepeat(1);
//                        primaryNode.getIterators().add(iterator);
//                        inputCellHandler.commitEdit(iterator.getDataNode(), values[i]);
//                    } else {
//                        inputCellHandler.commitEdit(primaryNode, values[i]);
//                        primaryNode.getIterators().get(0).setRepeat(1);
//                    }
//                }
//
//                // jump to first iteration
//                TestDataTreeItem child = new TestDataTreeItem(primaryNode);
//                if (primaryNode.getName().equals("RETURN"))
//                    child.setColumnType(TestDataTreeItem.ColumnType.INPUT);
//                else
//                    child.setColumnType(TestDataTreeItem.ColumnType.EXPECTED);
//                TreeItem<DataNode> current = getTreeTableRow().getTreeItem();
//                TreeItem<DataNode> parent = current.getParent();
//
//                int index = parent.getChildren().indexOf(current);
//                parent.getChildren().remove(index);
//                parent.getChildren().add(index, child);
//
//                TestCaseTreeTableController.loadChildren(testCase, child);
//
//            } else {
//                String content = "Multiple values entering is only supported on stub parameters and return variable";
//                UIController.showErrorDialog(content, "Test Data Entering", "Invalid value");
//            }
//
//        } else {
//            inputCellHandler.commitEdit(valueDataNode, newValue);
//            valueDataNode.getIterators().get(0).setRepeat(Iterator.FILL_ALL);
//        }
    }

    protected boolean isReturnRelated(ITestNode node) {
        if (node instanceof UnitTestNode || node instanceof LabelTestNode || node == null)
            return false;

        if (node.getTitle().equals("return") && node.getParent() instanceof SutTestNode) {
            return true;
        }

        return isReturnRelated(node.getParent());
    }

    public enum CellType {
        INPUT,
        EXPECTED,
    }
}