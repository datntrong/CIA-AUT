package uet.fit.aut.testdata.object;

import java.util.List;
import java.util.Set;

public interface IDataNode {
	/**
	 * The access operator to access element in class/struct
	 */
	String DOT_ACCESS = ".";
	String GETTER_METHOD = IDataNode.DOT_ACCESS + "get";
	String NULL_POINTER_IN_CPP = "nullptr";
	String NULL_POINTER_IN_C = "NULL";
	String ONE_LEVEL_POINTER_OPERATOR = "*";
	String REFERENCE_OPERATOR = "&";
	String SETTER_METHOD = IDataNode.DOT_ACCESS + "set";

	void addChild(IDataNode newChild);

	List<IDataNode> getChildren();

	void setChildren(List<IDataNode> children);

	/**
	 * Get the string used to put in google test file
	 */
	String getInputForGoogleTest(boolean isDeclared) throws Exception;

	Set<String> getAdditionalSources();

	String getName();

	String getDisplayName();

	void setName(String name);

	IDataNode getParent();

	void setParent(IDataNode parent);

	String getVirtualName();

	void setVirtualName(String virtualName);

	void setVirtualName();

	String getPathFromRoot();

	@Override
	String toString();

	IDataNode getRoot();

	IDataNode getUnit();
}