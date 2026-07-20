package uet.fit.aut.parser.obj;

import uet.fit.aut.parser.dependency.Dependency;
import uet.fit.aut.parser.dependency.IncludeHeaderDependency;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class UnspecifiedFileNode extends Node implements IFileNode {

	// true if the current node is analyzed include dependency before
	protected boolean includeHeaderDependencyState = false;

	@Override
	public File getFile() {
		return new File(getAbsolutePath());
	}

	public List<Dependency> getIncludeHeaderNodes() {
		return getDependencies().stream()
				.filter(d -> d instanceof IncludeHeaderDependency)
				.collect(Collectors.toList());
	}

	@Override
	public void setIncludeHeaderDependencyState(boolean includeHeaderDependencyState) {
		this.includeHeaderDependencyState = includeHeaderDependencyState;
	}

	@Override
	public boolean isIncludeHeaderDependencyState() {
		return includeHeaderDependencyState;
	}
}
