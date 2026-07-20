package uet.fit.aut.parser.dependency;

import org.eclipse.cdt.core.dom.ast.IASTPreprocessorIncludeStatement;
import org.jetbrains.annotations.Nullable;
import uet.fit.aut.parser.obj.IFileNode;
import uet.fit.aut.parser.obj.INode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractIncludeHeaderDependencyGeneration extends AbstractDependencyGeneration<IFileNode> {

	private final static Logger logger = LoggerFactory.getLogger(IncludeHeaderDependencyGeneration.class);

	protected synchronized void addDependency(INode owner, INode referredNode) {
		IncludeHeaderDependency d = new IncludeHeaderDependency(owner, referredNode);

		synchronized (owner.getDependencies()) {
			if (!owner.getDependencies().contains(d)) {
				owner.getDependencies().add(d);
			}
		}

		synchronized (referredNode.getDependencies()) {
			if (!referredNode.getDependencies().contains(d)) {
				referredNode.getDependencies().add(d);
			}
		}

		logger.debug("Found an include dependency: " + d);
	}

	protected abstract @Nullable IFileNode findIncludeNode(IASTPreprocessorIncludeStatement includeStm, IFileNode source);
}

