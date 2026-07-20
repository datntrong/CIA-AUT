package uet.fit.aut.search.condition;

import uet.fit.aut.parser.obj.AbstractFunctionNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.search.SearchCondition;

public class StaticAbstractFunctionNodeCondition extends SearchCondition {

    @Override
    public boolean isSatisfiable(INode n) {
        return n instanceof AbstractFunctionNode && ((AbstractFunctionNode) n).isStatic();
    }
}
