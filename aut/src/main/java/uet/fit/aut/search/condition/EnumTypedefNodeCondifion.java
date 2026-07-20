package uet.fit.aut.search.condition;

import uet.fit.aut.parser.obj.EnumTypedefNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.search.SearchCondition;

/**
 * Created by DucToan on 14/07/2017.
 */
public class EnumTypedefNodeCondifion extends SearchCondition {
    @Override
    public boolean isSatisfiable(INode n) {
        return n instanceof EnumTypedefNode;
    }
}
