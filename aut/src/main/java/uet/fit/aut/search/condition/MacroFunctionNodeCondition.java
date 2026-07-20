package uet.fit.aut.search.condition;

import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.parser.obj.MacroFunctionNode;
import uet.fit.aut.search.SearchCondition;

public class MacroFunctionNodeCondition extends SearchCondition {

    @Override
    public boolean isSatisfiable(INode n) {
        return n instanceof MacroFunctionNode;
    }
}
