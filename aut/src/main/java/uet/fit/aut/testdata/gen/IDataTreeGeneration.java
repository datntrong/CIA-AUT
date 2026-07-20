package uet.fit.aut.testdata.gen;


import uet.fit.aut.parser.obj.ICommonFunctionNode;
import uet.fit.aut.testdata.object.IDataNode;
import uet.fit.aut.testdata.object.RootDataNode;

import java.util.Map;

/**
 * A tree represent value of variables
 *
 * @author DucAnh
 */
public interface IDataTreeGeneration extends IGeneration {

	/**
	 * Generate tree of variables
	 *
	 * @throws Exception
	 */
	void generateTree() throws Exception;

	/**
	 * Get the corresponding function
	 *
	 * @return
	 */
	ICommonFunctionNode getFunctionNode();

	/**
	 * Set function node
	 *
	 * @param functionNode
	 */
	void setFunctionNode(ICommonFunctionNode functionNode);

	/**
	 * Get static solution
	 *
	 * @return
	 */
	Map<String, String> getValues();

	void setValues(Map<String, String> values);

	void setRoot(RootDataNode root);

	RootDataNode getRoot();

	void setVituralName(IDataNode n);

}