package uet.fit.aut.search.condition;

import uet.fit.aut.parser.obj.ExternalVariableNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.search.SearchCondition;

/**
 * Represent global or extern variable, e.g., "int MY_MAX_VALUE"
 *
 * @author TungLam
 */
public class GlobalVariableNodeCondition extends SearchCondition {

    @Override
    public boolean isSatisfiable(INode n) {
        return n instanceof ExternalVariableNode;
    }
}
