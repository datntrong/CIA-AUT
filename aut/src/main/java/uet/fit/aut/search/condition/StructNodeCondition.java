package uet.fit.aut.search.condition;

import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.parser.obj.SpecialStructTypedefNode;
import uet.fit.aut.parser.obj.StructNode;
import uet.fit.aut.parser.obj.StructTypedefNode;
import uet.fit.aut.search.SearchCondition;

/**
 * Demo a condition
 *
 * @author DucAnh
 */
public class StructNodeCondition extends SearchCondition {

    @Override
    public boolean isSatisfiable(INode n) {
        return n instanceof StructNode || n instanceof SpecialStructTypedefNode || n instanceof StructTypedefNode;
    }
}
