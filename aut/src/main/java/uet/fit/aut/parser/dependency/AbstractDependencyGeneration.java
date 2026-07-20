package uet.fit.aut.parser.dependency;

import uet.fit.aut.parser.obj.INode;

public abstract class AbstractDependencyGeneration<T extends INode> {

	protected abstract void dependencyGeneration(T root);
}
