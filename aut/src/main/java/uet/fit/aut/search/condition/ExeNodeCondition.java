package uet.fit.aut.search.condition;

import uet.fit.aut.parser.obj.ExeNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.search.SearchCondition;

public class ExeNodeCondition extends SearchCondition {

    @Override
    public boolean isSatisfiable(INode n) {
        return n instanceof ExeNode;
    }
}
