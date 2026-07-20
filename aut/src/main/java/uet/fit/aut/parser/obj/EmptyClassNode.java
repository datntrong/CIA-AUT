package uet.fit.aut.parser.obj;

import org.eclipse.cdt.core.dom.ast.IASTElaboratedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import uet.fit.aut.parser.dependency.DefinitionDependency;
import uet.fit.aut.parser.dependency.Dependency;

import java.io.File;

/**
 * Represent empty class declaration
 * <p>
 * <p>
 * 
 * <pre>
 * |    class Student;
 * </pre>
 *
 * @author ducanhnguyen
 */
public class EmptyClassNode extends ClassNode implements IEmptyStructureNode {

	private boolean analyzeDefinitionState;

	@Override
	public boolean isAnalyzeDefinitionState() {
		return analyzeDefinitionState;
	}

	@Override
	public void setAnalyzeDefinitionState(boolean analyzeDefinitionState) {
		this.analyzeDefinitionState = analyzeDefinitionState;
	}

	@Override
	public String getNewType() {
		return ((IASTElaboratedTypeSpecifier) getAST().getDeclSpecifier()).getName().toString();
	}

	@Override
	public IASTFileLocation getNodeLocation() {
		return ((IASTElaboratedTypeSpecifier) getAST().getDeclSpecifier()).getName().getFileLocation();
	}

	@Override
	public File getSourceFile() {
		return new File(getAST().getContainingFilename());
	}

	@Override
	public String toString() {
		return /* "class " + */ super.toString();
	}

	@Override
	public INode getDefinition() {
		for (Dependency d : getDependencies()) {
			if (d instanceof DefinitionDependency && d.getStartArrow().equals(this))
				return d.getEndArrow();
		}
		return null;
	}
}
