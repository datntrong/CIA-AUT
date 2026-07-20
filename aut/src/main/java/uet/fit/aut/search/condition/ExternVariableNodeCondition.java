package uet.fit.aut.search.condition;

import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.parser.obj.IVariableNode;
import uet.fit.aut.parser.obj.VariableNode;
import uet.fit.aut.search.SearchCondition;

/**
 * Represent extern variable, e.g., "extern int MY_MAX_VALUE"
 *
 * @author DucAnh
 */
public class ExternVariableNodeCondition extends SearchCondition {

    @Override
    public boolean isSatisfiable(INode n) {
        return n instanceof VariableNode && ((IVariableNode) n).isExtern();
    }
}
