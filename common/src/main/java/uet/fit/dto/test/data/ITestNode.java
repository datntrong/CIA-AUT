package uet.fit.dto.test.data;

import java.util.List;

public interface ITestNode {

	String getTitle();
	void setTitle(String title);

	List<ITestNode> getChildren();
	ITestNode getParent();

	void setParent(ITestNode parent);
	String getPath();

	String PATH_DELIMITER = "/";
	String EXPECT = "<<EXPECT>>";
}
