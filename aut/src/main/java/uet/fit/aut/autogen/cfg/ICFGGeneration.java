package uet.fit.aut.autogen.cfg;

import uet.fit.aut.parser.obj.IFunctionNode;

/**
 * This interface is used to generate CFG
 *
 * @author ducanh
 */
public interface ICFGGeneration {
	int IF_FLAG = 0;

	int DO_FLAG = 1;

	int WHILE_FLAG = 2;

	int FOR_FLAG = 3;

	/**
	 * Generate the control flow graph corresponding to the given function
	 *
	 * @return
	 */
	ICFG generateCFG();

	IFunctionNode getFunctionNode();

	void setFunctionNode(IFunctionNode functionNode);

}
