package uet.fit.client.ui.obj;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.dto.test.data.GlobalTestNode;
import uet.fit.dto.test.data.ITestNode;

public class TestDataGlobalVariableTreeItem extends ExpectableTestDataTreeItem{

    private final static Logger logger = LoggerFactory.getLogger(TestDataGlobalVariableTreeItem.class);

    public TestDataGlobalVariableTreeItem(ITestNode dataNode) {
        super(dataNode);

        if (dataNode.getParent() instanceof GlobalTestNode) {
            GlobalTestNode parent = (GlobalTestNode) dataNode.getParent();
            this.expectedNode = parent.getExpectedMap().get(dataNode);
            if (expectedNode == null) {
                logger.debug("Failed on get expected output node");
            }
        }
    }
}
