package uet.fit.aut.search.condition;

import uet.fit.aut.parser.obj.CloneVariableNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.parser.obj.VariableNode;
import uet.fit.aut.search.SearchCondition;

public class VariableNodeCondition extends SearchCondition {

    @Override
    public boolean isSatisfiable(INode n) {
        return n instanceof VariableNode && !(n instanceof CloneVariableNode);
    }
}
