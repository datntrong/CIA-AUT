package uet.fit.aut.search.condition;

import uet.fit.aut.parser.obj.*;
import uet.fit.aut.search.SearchCondition;

public class StructurevsTypedefCondition extends SearchCondition {

    @Override
    public boolean isSatisfiable(INode n) {
        if (n instanceof StructureNode || n instanceof ITypedefDeclaration)
            return true;
        else return n instanceof VariableNode && !(n.getParent() instanceof FunctionNode);
    }
}
