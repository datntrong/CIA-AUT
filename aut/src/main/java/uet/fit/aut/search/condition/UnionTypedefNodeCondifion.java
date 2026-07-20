package uet.fit.aut.search.condition;

import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.parser.obj.UnionTypedefNode;
import uet.fit.aut.search.SearchCondition;

/**
 * Created by DucToan on 14/07/2017.
 */
public class UnionTypedefNodeCondifion extends SearchCondition {
    @Override
    public boolean isSatisfiable(INode n) {
        return n instanceof UnionTypedefNode;
    }
}
