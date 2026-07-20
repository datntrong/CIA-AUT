package uet.fit.aut.search.condition;

import uet.fit.aut.parser.obj.EnumNode;
import uet.fit.aut.parser.obj.EnumTypedefNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.parser.obj.SpecialEnumTypedefNode;
import uet.fit.aut.search.SearchCondition;

/**
 * Demo a condition
 *
 * @author DucAnh
 */
public class EnumNodeCondition extends SearchCondition {

    @Override
    public boolean isSatisfiable(INode n) {
        return n instanceof EnumNode || n instanceof SpecialEnumTypedefNode || n instanceof EnumTypedefNode;
    }
}
