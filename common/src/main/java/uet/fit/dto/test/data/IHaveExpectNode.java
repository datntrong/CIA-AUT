package uet.fit.dto.test.data;

import java.util.List;
import java.util.Map;

public interface IHaveExpectNode {

	List<ITestNode> getExpectNodes();

	Map<ITestNode, ITestNode> getExpectedMap();
}
