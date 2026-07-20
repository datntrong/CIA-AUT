package uet.fit.client.ui.obj;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.client.utils.TestTreeUtils;
import uet.fit.dto.test.data.ITestNode;
import uet.fit.dto.test.data.SutTestNode;

public class TestDataParameterTreeItem extends ExpectableTestDataTreeItem {

    private final static Logger logger = LoggerFactory.getLogger(TestDataParameterTreeItem.class);

    public TestDataParameterTreeItem(ITestNode dataNode) {
        super(dataNode);

        SutTestNode sut = TestTreeUtils.findSubprogramUnderTest(dataNode);
        if (sut != null) {
            this.expectedNode = sut.getExpectedMap().get(dataNode);
            if (expectedNode == null) {
                logger.debug("Failed on get expected output node of " + dataNode.getPath());
            }
        }
    }
}
