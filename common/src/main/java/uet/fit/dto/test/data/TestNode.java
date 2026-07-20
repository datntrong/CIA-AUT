package uet.fit.dto.test.data;

import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public abstract class TestNode implements ITestNode {

	@Expose
	private String clazz = getClass().getName();

	@Expose
	private String title;

	private ITestNode parent;

	@Expose
	private List<ITestNode> children = new ArrayList<>();

	@Override
	public String getPath() {
		String path;

		ITestNode parent = getParent();

		if (parent == null) {
			path = getTitle();
		} else if (parent instanceof IHaveExpectNode) {
			path = parent.getPath() + PATH_DELIMITER;
			IHaveExpectNode haveExpectNode = (IHaveExpectNode) parent;
			if (haveExpectNode.getExpectNodes().contains(this))
				path += EXPECT + PATH_DELIMITER;
			path += getTitle();
		} else {
			path = parent.getPath() + PATH_DELIMITER + getTitle();
		}

		return path;
	}
}
