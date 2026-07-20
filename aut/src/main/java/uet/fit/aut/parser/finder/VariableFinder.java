package uet.fit.aut.parser.finder;


import uet.fit.aut.parser.obj.IFunctionNode;
import uet.fit.aut.parser.obj.IVariableNode;
import uet.fit.aut.search.Search;
import uet.fit.aut.search.condition.GlobalVariableNodeCondition;

import java.io.File;
import java.util.List;

/**
 * Find variable by name
 *
 * @author TungLam
 */
public class VariableFinder {

    /**
     * Node in the structure that contains the searched variable
     */
    private final IFunctionNode context;

    public VariableFinder(IFunctionNode context) {
        this.context = context;
    }

    public IVariableNode find(String variableName) {
        List<Level> spaces = new VariableSearchingSpace(context).getSpaces();
        List<IVariableNode> globalVars = Search.searchInSpace(spaces, new GlobalVariableNodeCondition(), File.separator + variableName);
        if (!globalVars.isEmpty())
            return globalVars.get(0);
        else
            return null;
    }


}
