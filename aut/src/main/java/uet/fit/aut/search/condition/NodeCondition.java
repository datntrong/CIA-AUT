package uet.fit.aut.search.condition;

import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.parser.obj.Node;
import uet.fit.aut.search.SearchCondition;

public class NodeCondition extends SearchCondition {

    @Override
    public boolean isSatisfiable(INode n) {
        return n instanceof Node;
    }
}
