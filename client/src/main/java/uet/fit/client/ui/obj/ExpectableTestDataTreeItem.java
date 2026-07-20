package uet.fit.client.ui.obj;

import javafx.scene.control.TreeItem;
import uet.fit.dto.test.data.ITestNode;

import java.util.ArrayList;
import java.util.List;

public abstract class ExpectableTestDataTreeItem extends TestDataTreeItem {

	protected ITestNode inputNode;
	protected ITestNode expectedNode;

	protected final List<TreeItem<ITestNode>> inputChildren = new ArrayList<>();
	protected final List<TreeItem<ITestNode>> expectedChildren = new ArrayList<>();

	protected ColumnType selectedColumn = ColumnType.INPUT;

	public ExpectableTestDataTreeItem(ITestNode dataNode) {
		super(dataNode);
		setColumnType(ColumnType.ALL);
		this.inputNode = dataNode;
	}

	public ColumnType getSelectedColumn() {
		return selectedColumn;
	}

	public void setSelectedColumn(ColumnType selectedColumn) {
		if (selectedColumn != this.selectedColumn) {
			if (this.selectedColumn == ColumnType.INPUT) {
				// save children
				inputChildren.clear();
				inputChildren.addAll(getChildren());

				// switch value and children
				setValue(expectedNode);
				getChildren().clear();
				getChildren().addAll(expectedChildren);
			} else if (this.selectedColumn == ColumnType.EXPECTED) {
				// save children
				expectedChildren.clear();
				expectedChildren.addAll(getChildren());

				// switch value and children
				setValue(inputNode);
				getChildren().clear();
				getChildren().addAll(inputChildren);
			}

			this.selectedColumn = selectedColumn;
		}
	}

	public ITestNode getInputNode() {
		return inputNode;
	}

	public ITestNode getExpectedNode() {
		return expectedNode;
	}

	public void setInputNode(ITestNode inputNode) {
		this.inputNode = inputNode;
	}

	public void setExpectedNode(ITestNode expectedNode) {
		this.expectedNode = expectedNode;
	}
}
