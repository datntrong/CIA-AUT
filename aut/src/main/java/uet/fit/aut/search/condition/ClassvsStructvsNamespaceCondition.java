package uet.fit.aut.search.condition;

import uet.fit.aut.parser.obj.ClassNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.parser.obj.NamespaceNode;
import uet.fit.aut.parser.obj.StructNode;
import uet.fit.aut.search.SearchCondition;

public class ClassvsStructvsNamespaceCondition extends SearchCondition {

    @Override
    public boolean isSatisfiable(INode n) {
        return n instanceof ClassNode || n instanceof StructNode || n instanceof NamespaceNode;
    }
}
