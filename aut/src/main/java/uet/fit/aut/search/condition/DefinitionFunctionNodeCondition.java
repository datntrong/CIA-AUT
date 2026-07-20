package uet.fit.aut.search.condition;

import uet.fit.aut.parser.obj.DefinitionFunctionNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.search.SearchCondition;

/**
 * Demo a condition
 *
 * @author DucAnh
 */
public class DefinitionFunctionNodeCondition extends SearchCondition {

    @Override
    public boolean isSatisfiable(INode n) {
        return n instanceof DefinitionFunctionNode;
    }
}
