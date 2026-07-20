package uet.fit.aut.search.condition;

import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.parser.obj.IncludeHeaderNode;
import uet.fit.aut.search.SearchCondition;

public class IncludeHeaderNodeCondition extends SearchCondition {

    @Override
    public boolean isSatisfiable(INode n) {
        return n instanceof IncludeHeaderNode;
    }
}
