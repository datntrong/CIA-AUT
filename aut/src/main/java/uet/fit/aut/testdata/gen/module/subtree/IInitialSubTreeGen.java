package uet.fit.aut.testdata.gen.module.subtree;

import uet.fit.aut.parser.funcdetail.IFunctionDetailTree;
import uet.fit.aut.parser.obj.VariableNode;
import uet.fit.aut.testdata.object.DataNode;
import uet.fit.aut.testdata.object.RootDataNode;
import uet.fit.aut.testdata.object.ValueDataNode;

/**
 * Xay dung nhanh (sub tree).
 * Ex: GLOBAL, UUT or STUB.
 *
 * @author TungLam
 */
public interface IInitialSubTreeGen {

    /**
     * Generate a complete data tree given a function
     * @param root
     * @param functionTree
     * @throws Exception
     */
    void generateCompleteTree(RootDataNode root, IFunctionDetailTree functionTree) throws Exception;

    /**
     * Generate a partial data tree given a variable
     * @param vCurrentChild
     * @param nCurrentParent
     * @throws Exception
     */
    ValueDataNode genInitialTree(VariableNode vCurrentChild, DataNode nCurrentParent) throws Exception;

//    /**
//     * Set the vitural name of node
//     *
//     * @param n
//     */
//    void setVituralName(IDataNode n);
}
