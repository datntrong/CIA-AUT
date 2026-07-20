package uet.fit.aut.parser.dependency;

import uet.fit.aut.parser.obj.INode;

public class TypeDependency extends Dependency {

    public TypeDependency(INode startArrow, INode endArrow) {
        super(startArrow, endArrow);
    }

    public TypeDependency() {}
}
