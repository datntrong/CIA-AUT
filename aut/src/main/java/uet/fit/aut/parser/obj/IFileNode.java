package uet.fit.aut.parser.obj;

import uet.fit.aut.parser.dependency.Dependency;

import java.util.List;

public interface IFileNode extends IHasFileNode, INode {
	List<Dependency> getIncludeHeaderNodes();
	boolean isIncludeHeaderDependencyState();
	void setIncludeHeaderDependencyState(boolean includeHeaderDependencyState);
}
