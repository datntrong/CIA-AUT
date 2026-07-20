package uet.fit.aut.parser.vardect;

import uet.fit.aut.parser.obj.IFunctionNode;
import uet.fit.aut.parser.obj.IVariableNode;
import uet.fit.aut.search.ISearch;

import java.util.List;

/**
 * Find all external variables of a function
 *
 * @author ducanhnguyen
 */
public interface IVariableDetecter extends ISearch {
    /**
     * Find external variables of a function
     *
     * @return
     */
    List<IVariableNode> findVariables();

    IFunctionNode getFunction();

    void setFunction(IFunctionNode function);
}