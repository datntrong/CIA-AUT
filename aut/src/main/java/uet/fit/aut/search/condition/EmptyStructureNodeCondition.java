package uet.fit.aut.search.condition;

import uet.fit.aut.parser.obj.IEmptyStructureNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.search.SearchCondition;

/**
 * @author Lamnt
 */
public class EmptyStructureNodeCondition extends SearchCondition {

    @Override
    public boolean isSatisfiable(INode n) {
        return (n instanceof IEmptyStructureNode);
    }
}
