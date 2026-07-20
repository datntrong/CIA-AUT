package uet.fit.client.ui.view;

import javafx.css.PseudoClass;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableRow;
import uet.fit.client.ui.obj.TestDataGlobalVariableTreeItem;
import uet.fit.client.ui.obj.TestDataParameterTreeItem;
import uet.fit.client.ui.obj.TestDataTreeItem;
import uet.fit.client.utils.TestTreeUtils;
import uet.fit.dto.test.data.EditableTestNode;
import uet.fit.dto.test.data.ITestNode;

/**
 * Represents a single row/column in the test case tab
 */
public class ExpectedColumnCell extends AbstractTableCell {

	public static final PseudoClass PSEUDO_CLASS_SELECTED = PseudoClass.getPseudoClass("expected-output-column-is-selected");
	private static final PseudoClass PSEUDO_CLASS = PseudoClass.getPseudoClass("expected-output-column-parameter");

	@Override
	public void startEdit() {
		logger.debug("Start editing on the cell at line " + this.getIndex());
		super.startEdit();

		saveValueWhenUsersPressEnter();

		if (getTreeTableRow().getTreeItem() instanceof TestDataTreeItem) {
			TestDataTreeItem testDataTreeItem = (TestDataTreeItem) getTreeTableRow().getTreeItem();
			TestDataTreeItem.ColumnType columnType = testDataTreeItem.getColumnType();

			// if the value is parameter of Stub Function, and not is a return node
			if (!(columnType.equals(TestDataTreeItem.ColumnType.EXPECTED) && testDataTreeItem.getValue().getTitle().equals("return"))) {
				if (columnType != TestDataTreeItem.ColumnType.INPUT) {
					showText(CellType.EXPECTED);
				}
			}
		}

		escapePressed = false;
		tablePos = getTreeTableView().getEditingCell();
	}

	@Override
	protected void doCommitEdit(String newValue) {
		TreeTableRow<ITestNode> row = getTreeTableRow();
		ITestNode dataNode = row.getItem();
		TreeItem<ITestNode> treeItem = row.getTreeItem();

		if (dataNode != null) {
			ITestNode expectedNode;

			if (treeItem instanceof TestDataParameterTreeItem) {
				TestDataParameterTreeItem parameterTreeItem = (TestDataParameterTreeItem) treeItem;

				if (dataNode.getTitle().equals("return")) {
					expectedNode = dataNode;
				} else {
					expectedNode = parameterTreeItem.getExpectedNode();
				}

			} else if (treeItem instanceof TestDataGlobalVariableTreeItem) {
				TestDataGlobalVariableTreeItem globalVarTreeItem = (TestDataGlobalVariableTreeItem) treeItem;
				expectedNode = globalVarTreeItem.getExpectedNode();

			} else {
				expectedNode = dataNode;
			}

			// clear value
			if (newValue == null || newValue.isEmpty()) {
				clearValue(expectedNode);
			}
			// commit edit new value
			else if (expectedNode instanceof EditableTestNode) {
				onRetrieveValue((EditableTestNode) expectedNode, newValue);
			}

			// TODO: assert
//				if (expectedNode instanceof NormalDataNode || expectedNode instanceof EnumDataNode) {
//					expectedNode.getIterators().forEach(i -> i.getDataNode().setAssertMethod(AssertMethod.ASSERT_EQUAL));
////                        expectedNode.setAssertMethod(AssertMethod.ASSERT_EQUAL);
//					if (actualNode != null)
//						actualNode.getIterators().forEach(i -> i.getDataNode().setAssertMethod(AssertMethod.ASSERT_EQUAL));
////                            actualNode.setAssertMethod(AssertMethod.ASSERT_EQUAL);
//				}
		} else {
			logger.debug("There is matching between a cell and its data");
		}
	}

	@Override
	public void updateItem(String item, boolean empty) {
		super.updateItem(item, empty);

		pseudoClassStateChanged(PSEUDO_CLASS, false);

		TreeItem<ITestNode> treeItem = getTreeTableRow().getTreeItem();
		if (treeItem != null && treeItem.getValue() != null) {
			if (treeItem instanceof TestDataTreeItem
					&& ((TestDataTreeItem) treeItem).getColumnType().equals(TestDataTreeItem.ColumnType.EXPECTED)
					&& treeItem.getValue().getTitle().equals("return")
					&& !treeItem.getValue().getPath().contains("<<SBF>>")
					&& !treeItem.getValue().getPath().contains("<<STUB>>")) {
				setEditable(false);
				setText(null);
				setGraphic(null);
				return;
			}

			if (treeItem instanceof TestDataTreeItem
					&& ((TestDataTreeItem) treeItem).getColumnType() == TestDataTreeItem.ColumnType.INPUT) {
				setEditable(false);
				setText(null);
				setGraphic(null);
				pseudoClassStateChanged(PSEUDO_CLASS, !TestTreeUtils.isStubRelated(treeItem.getValue()));
				return;
			}

			if (treeItem instanceof TestDataParameterTreeItem || treeItem instanceof TestDataGlobalVariableTreeItem) {
				pseudoClassStateChanged(PSEUDO_CLASS, true);
			} else if (treeItem instanceof TestDataTreeItem) {
				TestDataTreeItem.ColumnType columnType = ((TestDataTreeItem) treeItem).getColumnType();
				if (columnType == TestDataTreeItem.ColumnType.EXPECTED) {
					pseudoClassStateChanged(PSEUDO_CLASS, true);
				}
			}

			ITestNode dataNode = treeItem.getValue();

			// if the dataNode is a parameter, show value of Expected Output
			if (treeItem instanceof TestDataParameterTreeItem) {
				if (dataNode.getTitle().equals("return")) {
					update(dataNode);
				} else {
					ITestNode expectedOutput = ((TestDataParameterTreeItem) treeItem).getExpectedNode();
					update(expectedOutput);
				}
			} else if (treeItem instanceof TestDataGlobalVariableTreeItem) {
				ITestNode expectedOutput = ((TestDataGlobalVariableTreeItem) treeItem).getExpectedNode();
				update(expectedOutput);
			}
			// TODO: template function
//			else if (treeItem.getValue() instanceof TemplateSubprogramDataNode) {
//				// show nothing
//			}
			else {
				update(dataNode);
			}

		} else {
			setText(null);
			setGraphic(null);
		}
	}
}