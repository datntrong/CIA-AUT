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
import uet.fit.dto.test.data.LabelTestNode;
import uet.fit.dto.test.data.SutTestNode;
import uet.fit.dto.test.data.UnitTestNode;

/**
 * Represents a single row/column in the test case tab
 */
public class InputColumnCell extends AbstractTableCell {

	public static final PseudoClass PSEUDO_CLASS = PseudoClass.getPseudoClass("input-column-parameter");
	public static final PseudoClass PSEUDO_CLASS_SELECTED = PseudoClass.getPseudoClass("input-column-is-selected");

	@Override
	public void startEdit() {
		logger.debug("Start editing on the cell at line " + this.getIndex());
		super.startEdit();

		saveValueWhenUsersPressEnter();

		TreeItem<ITestNode> treeItem = getTreeTableRow().getTreeItem();

		if (treeItem instanceof TestDataTreeItem) {
			TestDataTreeItem testDataTreeItem = (TestDataTreeItem) treeItem;
			TestDataTreeItem.ColumnType columnType = testDataTreeItem.getColumnType();
			if (columnType != TestDataTreeItem.ColumnType.EXPECTED) {
				showText(CellType.INPUT);
			} else if (testDataTreeItem.getValue() != null && testDataTreeItem.getValue().getTitle().equals("return")) {
				// if testDataTreeItem is RETURN parameter of Stub function
				showText(CellType.INPUT);
			}
		}
		// TODO: template function
//		else if (treeItem != null && treeItem.getValue() instanceof TemplateSubprogram) {
//			showText(CellType.INPUT);
//		}

		escapePressed = false;
		tablePos = getTreeTableView().getEditingCell();
	}

	@Override
	protected void doCommitEdit(String newValue) {
//		// TODO: update status of testcase
//		if (testCase instanceof TestCase)
//			((TestCase) testCase).setStatus(TestCase.STATUS_NA);

		TreeTableRow<ITestNode> row = getTreeTableRow();
		ITestNode dataNode = row.getItem();

		if (dataNode == null) {
			logger.debug("There is matching between a cell and its data");
		}

		// clear value
		if (newValue == null || newValue.isEmpty()) {
			clearValue(dataNode);
		}
		// commit value
		else if (dataNode instanceof EditableTestNode) {
			onRetrieveValue((EditableTestNode) dataNode, newValue);
		}
	}

//	private boolean isCommitInstanceOfSut(DataNode dataNode) {
//		ICommonFunctionNode sut = testCase.getFunctionNode();
//
//		INode realParent = sut.getParent();
//
//		if (sut instanceof IFunctionNode && ((IFunctionNode) sut).getRealParent() != null)
//			realParent = ((IFunctionNode) sut).getRealParent();
//
//		if (realParent instanceof ClassNode && sut.isTemplate()) {
//			if (dataNode instanceof TemplateClassDataNode && !dataNode.getChildren().isEmpty()
//					&& ((TemplateClassDataNode) dataNode).getCorrespondingVar() instanceof InstanceVariableNode) {
//				INode classNode = ((TemplateClassDataNode) dataNode).getCorrespondingType();
//				return classNode.equals(realParent.getParent());
//			}
//		}
//
//		return false;
//	}

//	private TreeItem<DataNode> getSutTreeItem(TreeItem<DataNode> current) {
//		// STEP 1: get root
//		TreeItem<DataNode> root = current;
//		while (root.getParent() != null)
//			root = root.getParent();
//
//		// STEP 2: get unit under test
//		TreeItem<DataNode> uut = null;
//		for (TreeItem<DataNode> child : root.getChildren()) {
//			DataNode node = child.getValue();
//			if (node instanceof UnitUnderTestNode) {
//				uut = child;
//				break;
//			}
//		}
//
//		// STEP 3: get subprogram under test
//		INode sut = testCase.getFunctionNode();
//		if (uut != null) {
//			for (TreeItem<DataNode> child : uut.getChildren()) {
//				DataNode node = child.getValue();
//				if (node instanceof SubprogramNode && ((SubprogramNode) node).getFunctionNode().equals(sut)) {
//					return child;
//				}
//			}
//		}
//
//		return null;
//	}

	@Override
	public void updateItem(String item, boolean empty) {
		super.updateItem(item, empty);

		pseudoClassStateChanged(PSEUDO_CLASS, false);

		TreeItem<ITestNode> treeItem = getTreeTableRow().getTreeItem();

		if (treeItem != null && treeItem.getValue() != null) {
			// if the tree item is child of a Parameter tree item, disable expected output column
			if (treeItem instanceof TestDataTreeItem
					&& ((TestDataTreeItem) treeItem).getColumnType() == TestDataTreeItem.ColumnType.EXPECTED
					&& !isReturnRelated(treeItem.getValue())) {
				setEditable(false);
				setText(null);
				setGraphic(null);
				pseudoClassStateChanged(PSEUDO_CLASS, !TestTreeUtils.isStubRelated(treeItem.getValue()));
				return;
			}

			if (treeItem instanceof TestDataParameterTreeItem) {
				if (!treeItem.getValue().getTitle().equals("return")) {
					pseudoClassStateChanged(PSEUDO_CLASS, true);
				}
			} else if (treeItem instanceof TestDataGlobalVariableTreeItem) {
				pseudoClassStateChanged(PSEUDO_CLASS, true);
			} else if (treeItem instanceof TestDataTreeItem) {
				TestDataTreeItem.ColumnType columnType = ((TestDataTreeItem) treeItem).getColumnType();
				if (columnType == TestDataTreeItem.ColumnType.INPUT) {
					pseudoClassStateChanged(PSEUDO_CLASS, true);
				}
			}

			ITestNode dataNode = treeItem.getValue();

			if (treeItem instanceof TestDataParameterTreeItem) {
				if (!dataNode.getTitle().equals("return")) {
					ITestNode inputDataNode = ((TestDataParameterTreeItem) treeItem).getInputNode();
					update(inputDataNode);
				}
			} else if (treeItem instanceof TestDataGlobalVariableTreeItem) {
				ITestNode inputDataNode = ((TestDataGlobalVariableTreeItem) treeItem).getInputNode();
				update(inputDataNode);
			}
			// disable input when the variable is return variable
			else if (TestTreeUtils.isExpected(dataNode)) {
//				disable();
			} else {
				update(dataNode);
			}

		} else {
			setText(null);
			setGraphic(null);
		}
	}
}
