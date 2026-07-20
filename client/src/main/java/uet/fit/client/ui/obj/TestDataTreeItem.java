package uet.fit.client.ui.obj;

import javafx.scene.control.TreeItem;
import uet.fit.dto.test.data.ITestNode;

public class TestDataTreeItem extends TreeItem<ITestNode> {

    private ColumnType columnType;

    public TestDataTreeItem(ColumnType columnType) {
        this.columnType = columnType;
    }

    public TestDataTreeItem(ITestNode dataNode) {
        super(dataNode);
        this.columnType = ColumnType.NONE;
    }

    public ColumnType getColumnType() {
        return columnType;
    }

    public void setColumnType(ColumnType columnType) {
        this.columnType = columnType;
    }

    public enum ColumnType {
        NONE,
        ALL,
        INPUT,
        EXPECTED
    }
}
