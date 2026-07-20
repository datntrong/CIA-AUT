package uet.fit.aut.search.condition;

import uet.fit.aut.parser.obj.CFileNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.search.SearchCondition;

public class CFileNodeCondition extends SearchCondition {

    @Override
    public boolean isSatisfiable(INode n) {
        return n instanceof CFileNode;
    }
}
