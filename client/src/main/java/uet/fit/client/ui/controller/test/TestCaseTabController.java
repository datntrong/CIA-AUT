package uet.fit.client.ui.controller.test;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import org.jetbrains.annotations.NotNull;
import uet.fit.client.ui.obj.ExpectableTestDataTreeItem;
import uet.fit.client.ui.obj.TestDataGlobalVariableTreeItem;
import uet.fit.client.ui.obj.TestDataParameterTreeItem;
import uet.fit.client.ui.obj.TestDataTreeItem;
import uet.fit.client.ui.view.AssertColumnCell;
import uet.fit.client.ui.view.ExpectedColumnCell;
import uet.fit.client.ui.view.InputColumnCell;
import uet.fit.client.ui.view.ParameterColumnCell;
import uet.fit.client.utils.TestTreeUtils;
import uet.fit.dto.test.data.HaveTypeTestNode;
import uet.fit.dto.test.data.ITestNode;
import uet.fit.dto.test.data.LabelTestNode;
import uet.fit.dto.test.data.StubSubprogramTestNode;
import uet.fit.dto.test.data.SutTestNode;
import uet.fit.dto.test.data.TestDataDTO;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import static uet.fit.client.utils.TestTreeUtils.isGlobalVariable;
import static uet.fit.client.utils.TestTreeUtils.isStubFunction;

public class TestCaseTabController implements Initializable {

	@FXML
	private TreeTableView<ITestNode> table;
	@FXML
	private  TreeTableColumn<ITestNode, String> colParam;
	@FXML
	private  TreeTableColumn<ITestNode, String> colType;
	@FXML
	private  TreeTableColumn<ITestNode, String> colExpect;
	@FXML
	private  TreeTableColumn<ITestNode, String> colInput;
	@FXML
	private  TreeTableColumn<ITestNode, String> colAssert;

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {
		table.setRowFactory(new TestDataRowFactory());
		colParam.setCellValueFactory(new TreeItemPropertyValueFactory<>("title"));
		colParam.setCellFactory(new ParameterColumnFactory());
		colType.setCellValueFactory(new TypeColumnFactory());
		colInput.setCellFactory(new InputColumnCellFactory());
		colExpect.setCellFactory(new ExpectedColumnCellFactory());
		colAssert.setCellFactory(new AssertColumnCellFactory());
	}

	public void loadTestCase(@NotNull TestDataDTO testData) {
		loadTreeTable(table, testData);
	}

	public static void loadTreeTable(@NotNull TreeTableView<ITestNode> treeTable, @NotNull TestDataDTO testData) {
		treeTable.setEditable(testData.isEditable());

		TreeItem<ITestNode> root = treeTable.getRoot();
		// create root first time
		if (root == null) {
			root = new TestDataTreeItem(testData);
			treeTable.setRoot(root);
		}

		loadChildren(root, testData);
		treeTable.refresh();
	}

	private static void loadChildren(TreeItem<ITestNode> treeItem, ITestNode node) {
		if (treeItem == null)
			return;

		// update node value
		treeItem.setValue(node);

		// check visibility
		if (isHide(node)) {
			treeItem.getChildren().clear();
			return;
		}

		// remove redundant nodes
		List<ITestNode> children = new ArrayList<>(node.getChildren());
		List<TreeItem<ITestNode>> itemChildren = new ArrayList<>(treeItem.getChildren());
		for (TreeItem<ITestNode> itemChild : itemChildren) {
			String title = itemChild.getValue().getTitle();
			ITestNode existedNode = children.stream()
					.filter(n -> n.getTitle().equals(title))
					.findFirst()
					.orElse(null);
			if (existedNode == null) {
				treeItem.getChildren().remove(itemChild);
			} else {
				children.remove(existedNode);
			}
		}

		// update children
		TestDataTreeItem.ColumnType defaultType = guessColumnType(treeItem);
		for (ITestNode child : node.getChildren()) {
			loadChild(treeItem, child, defaultType);
		}
	}

	private static void loadChild(TreeItem<ITestNode> parentTreeItem, ITestNode child, TestDataTreeItem.ColumnType defaultType) {
		// check visibility
		if (isHide(child))
			return;

		// find existed item
		TreeItem<ITestNode> item = parentTreeItem.getChildren().stream()
				.filter(i -> i.getValue() != null && i.getValue().getTitle().equals(child.getTitle()))
				.findFirst()
				.orElse(null);

		// item has already existed
		if (item != null) {
			// item is expectectable tree item
			// Eg: global variables, sut parameters
			if (item instanceof ExpectableTestDataTreeItem) {
				ExpectableTestDataTreeItem expectableItem = (ExpectableTestDataTreeItem) item;

				// find expected node
				ITestNode expectedNode = TestTreeUtils.getExpectedValue(child);

				// update input & expected node
				expectableItem.setInputNode(child);
				expectableItem.setExpectedNode(expectedNode);

				// node children for corresponding column
				if (expectableItem.getSelectedColumn() == TestDataTreeItem.ColumnType.EXPECTED) {
					if (expectedNode.getChildren() != null) {
						loadChildren(item, expectedNode);
					}
				} else {
					if (child.getChildren() != null) {
						loadChildren(item, child);
					}
				}

			} else {
				if (child.getChildren() != null) {
					loadChildren(item, child);
				}
			}

		}
		// item hasn't created yet
		else {
			item = new TestDataTreeItem(child);

			TestDataTreeItem dataItem = (TestDataTreeItem) item;
			if (isStubFunction(parentTreeItem.getValue())) {
				if (child.getTitle().equals("return")) {
					dataItem.setColumnType(TestDataTreeItem.ColumnType.INPUT);
				} else
					dataItem.setColumnType(TestDataTreeItem.ColumnType.EXPECTED);
			} else {
				dataItem.setColumnType(defaultType);
			}

			if (child instanceof StubSubprogramTestNode)
				item = new CheckBoxTreeItem<>(child);
			else if (child instanceof HaveTypeTestNode) {
				if (isParameter(child))
					item = new TestDataParameterTreeItem(child);
				else if (isGlobalVariable(child))
					item = new TestDataGlobalVariableTreeItem(child);
			}

			parentTreeItem.getChildren().add(item);

			if (child.getChildren() != null) {
				loadChildren(item, child);
			}
		}
	}

	/**
	 * Guessing column type
	 * @param treeItem parent item
	 * @return ColumnType
	 */
	private static TestDataTreeItem.ColumnType guessColumnType(TreeItem<ITestNode> treeItem) {
		TestDataTreeItem.ColumnType columnType = TestDataTreeItem.ColumnType.NONE;

		if (treeItem instanceof TestDataTreeItem) {
			columnType = ((TestDataTreeItem) treeItem).getColumnType();

			if (treeItem instanceof TestDataParameterTreeItem) {
				// if the tree item is of RETURN variable then its children is column type expected
				if (treeItem.getValue().getTitle().equals("return")) {
					columnType = TestDataTreeItem.ColumnType.EXPECTED;
				} else {
					columnType = ((TestDataParameterTreeItem) treeItem).getSelectedColumn();
				}
			} else if (treeItem instanceof TestDataGlobalVariableTreeItem) {
				columnType = ((TestDataGlobalVariableTreeItem) treeItem).getSelectedColumn();
			}
			// number of call node
			else if (treeItem.getParent() instanceof CheckBoxTreeItem) {
				columnType = TestDataTreeItem.ColumnType.NONE;
			}
		} else if (treeItem instanceof CheckBoxTreeItem) {
			columnType = TestDataTreeItem.ColumnType.EXPECTED;
		}

		return columnType;
	}

	private static boolean isParameter(ITestNode dataNode) {
		ITestNode parent = dataNode.getParent();
		if (parent instanceof SutTestNode) {
			return true;
		} else if (parent instanceof LabelTestNode) {
			return parent.getTitle().equals("<<STATIC>>");
		}

		return false;
	}

	private static boolean isHide(ITestNode node) {
		if (node == null)
			return true;

		if (node instanceof LabelTestNode) {
			return node.getChildren().isEmpty();
		}

		return false;
	}

	private static class TypeColumnFactory implements Callback<TreeTableColumn.CellDataFeatures<ITestNode, String>, ObservableValue<String>> {
		@Override
		public ObservableValue<String> call(TreeTableColumn.CellDataFeatures<ITestNode, String> cellDataFeatures) {
			ITestNode testNode = cellDataFeatures.getValue().getValue();
			SimpleStringProperty property = new SimpleStringProperty();
			if (testNode instanceof HaveTypeTestNode
					&& !testNode.getTitle().equals("Number of calls")) {
				property.setValue(((HaveTypeTestNode) testNode).getType());
			} else if (testNode instanceof StubSubprogramTestNode) {
				property.setValue(((StubSubprogramTestNode) testNode).getReturnType());
			}
			return property;
		}
	}

	private static class TestDataRowFactory implements Callback<TreeTableView<ITestNode>, TreeTableRow<ITestNode>> {
		@Override
		public TreeTableRow<ITestNode> call(TreeTableView<ITestNode> treeTableView) {
			final TreeTableRow<ITestNode> row = new TreeTableRow<>();
			row.treeItemProperty().addListener((observableValue, old, newValue) -> {
				if (newValue != null) {
					row.onScrollProperty().addListener((observableVal, eventHandler, t1) -> row.getTreeTableView().refresh());
					row.getTreeItem().expandedProperty().addListener((observableVal, b1, b2) -> row.getTreeTableView().refresh());
				}
			});
			return row;
		}
	}

	private static class InputColumnCellFactory implements Callback<TreeTableColumn<ITestNode, String>, TreeTableCell<ITestNode, String>> {
		@Override
		public TreeTableCell<ITestNode, String> call(TreeTableColumn<ITestNode, String> column) {
			InputColumnCell cell = new InputColumnCell();

			cell.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
				TreeItem<ITestNode> treeItem = cell.getTreeTableRow().getTreeItem();
				if (treeItem instanceof ExpectableTestDataTreeItem) {
					if (!treeItem.getValue().getTitle().equals("return")) {
						if (((ExpectableTestDataTreeItem) treeItem).getSelectedColumn() != TestDataTreeItem.ColumnType.INPUT) {
							((ExpectableTestDataTreeItem) treeItem).setSelectedColumn(TestDataTreeItem.ColumnType.INPUT);
							cell.getTreeTableView().refresh();
						}
					}
				}

				if (treeItem instanceof TestDataTreeItem && treeItem.getValue() != null) {
					if (!((TestDataTreeItem) treeItem).getColumnType()
							.equals(TestDataTreeItem.ColumnType.EXPECTED)) { // if not expected output column
						ContextMenu contextMenu = cell.setupContextMenu(treeItem.getValue());
						if (!contextMenu.getItems().isEmpty()
//                            && !treeItem.getValue().getName().equals("RETURN")
						) {
							cell.setContextMenu(contextMenu);
						}
					}
				}
			});

			return cell;
		}
	}

	private static class ExpectedColumnCellFactory implements Callback<TreeTableColumn<ITestNode, String>, TreeTableCell<ITestNode, String>> {
		@Override
		public TreeTableCell<ITestNode, String> call(TreeTableColumn<ITestNode, String> column) {
			ExpectedColumnCell cell = new ExpectedColumnCell();
			cell.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
				TreeItem<ITestNode> treeItem = cell.getTreeTableRow().getTreeItem();
				if (treeItem instanceof ExpectableTestDataTreeItem) {
					if (!treeItem.getValue().getTitle().equals("return")) {
						if (((ExpectableTestDataTreeItem) treeItem).getSelectedColumn() != TestDataTreeItem.ColumnType.EXPECTED) {
							((ExpectableTestDataTreeItem) treeItem).setSelectedColumn(TestDataTreeItem.ColumnType.EXPECTED);
							cell.getTreeTableView().refresh();
							// lazy load
							if (treeItem.getChildren().isEmpty()) {
								loadChildren(treeItem, treeItem.getValue());
							}
						}
					}
				}

				if (treeItem != null && treeItem.getValue() != null) {
					if (treeItem instanceof TestDataTreeItem) {
						if (!((TestDataTreeItem) treeItem).getColumnType()
								.equals(TestDataTreeItem.ColumnType.INPUT)) { // if not input column
							ContextMenu contextMenu = cell.setupContextMenu(treeItem.getValue());
							if (!contextMenu.getItems().isEmpty()) {
								cell.setContextMenu(contextMenu);
							}
						}
					}
				}
			});

			return cell;
		}
	}

	private static class AssertColumnCellFactory implements Callback<TreeTableColumn<ITestNode, String>, TreeTableCell<ITestNode, String>> {

		@Override
		public TreeTableCell<ITestNode, String> call(TreeTableColumn<ITestNode, String> param) {
			AssertColumnCell cell = new AssertColumnCell();

			cell.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
				TreeItem<ITestNode> treeItem = cell.getTreeTableRow().getTreeItem();
				if (treeItem instanceof ExpectableTestDataTreeItem) {
					if (!treeItem.getValue().getTitle().equals("return")) {
						if (((ExpectableTestDataTreeItem) treeItem).getSelectedColumn() != TestDataTreeItem.ColumnType.INPUT) {
							((ExpectableTestDataTreeItem) treeItem).setSelectedColumn(TestDataTreeItem.ColumnType.INPUT);
							cell.getTreeTableView().refresh();
						}
					}
				}
			});

			return cell;
		}
	}

	private static class ParameterColumnFactory implements Callback<TreeTableColumn<ITestNode, String>, TreeTableCell<ITestNode, String>> {

		@Override
		public TreeTableCell<ITestNode, String> call(TreeTableColumn<ITestNode, String> iTestNodeStringTreeTableColumn) {
			return new ParameterColumnCell();
		}
	}
}
