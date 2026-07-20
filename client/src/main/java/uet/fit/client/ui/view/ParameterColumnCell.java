package uet.fit.client.ui.view;

import javafx.scene.control.CheckBox;
import javafx.scene.control.TreeTableCell;
import uet.fit.dto.test.data.ITestNode;
import uet.fit.dto.test.data.StubSubprogramTestNode;

/**
 * Represents a single row/column in the test case tab
 */
public class ParameterColumnCell extends TreeTableCell<ITestNode, String> {

	@Override
	protected void updateItem(String item, boolean empty) {
		super.updateItem(item, empty);

		if (item != null && !empty) {
			setText(item);

			ITestNode node = getTreeTableRow().getItem();
			if (node instanceof StubSubprogramTestNode) {
				StubSubprogramTestNode stubSub = (StubSubprogramTestNode) node;
				CheckBox checkBox = new CheckBox();
				checkBox.setSelected(stubSub.isStub());
				checkBox.setDisable(true);
				setGraphic(checkBox);
			} else {
				setGraphic(null);
			}
		} else {
			setText(null);
			setGraphic(null);
		}
	}

}
