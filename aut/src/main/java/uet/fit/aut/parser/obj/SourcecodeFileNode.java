package uet.fit.aut.parser.obj;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import uet.fit.aut.parser.dependency.Dependency;
import uet.fit.aut.parser.dependency.IncludeHeaderDependency;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public abstract class SourcecodeFileNode extends Node implements IFileNode, ISourcecodeFileNode {

	// true if the current node is analyzed include dependency before
	protected boolean includeHeaderDependencyState = false;

	// true if the current node is expaned down to method level before
	protected boolean expandedToMethodLevelState = false;

	protected Date lastModifiedDate;

	protected String md5;

	@Override
	public List<Dependency> getIncludeHeaderNodes() {
		return getDependencies().stream()
				.filter(d -> d instanceof IncludeHeaderDependency)
				.collect(Collectors.toList());
	}

//	protected N AST;
//
//	@Override
//	public N getAST() {
//		return this.AST;
//	}
//
//	@Override
//	public void setAST(IASTTranslationUnit aST) {
//		this.AST = (N) aST;
//	}

	@Override
	public File getFile() {
		return new File(getAbsolutePath());
	}

	public String getMd5() {
		return md5;
	}

	public void setMd5(String md5) {
		this.md5 = md5;
	}

	public boolean isIncludeHeaderDependencyState() {
		return includeHeaderDependencyState;
	}

	public void setIncludeHeaderDependencyState(boolean includeHeaderDependencyState) {
		this.includeHeaderDependencyState = includeHeaderDependencyState;
	}

	public boolean isExpandedToMethodLevelState() {
		return expandedToMethodLevelState;
	}

	public void setExpandedToMethodLevelState(boolean expandedToMethodLevelState) {
		this.expandedToMethodLevelState = expandedToMethodLevelState;
	}

	public Date getLastModifiedDate() {
		return lastModifiedDate;
	}

	public void setLastModifiedDate(Date lastModifiedDate) {
		this.lastModifiedDate = lastModifiedDate;
	}
}

