package uet.fit.aut.parser.obj;

import org.eclipse.cdt.core.dom.ast.IASTElaboratedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import uet.fit.aut.parser.dependency.DefinitionDependency;
import uet.fit.aut.parser.dependency.Dependency;

import java.io.File;

/**
 * For example, <br/>
 * <p>
 * <p>
 * 
 * <pre>
 * 		union RGBA;
 * </pre>
 *
 * @author ducanhnguyen
 */
public class EmptyUnionNode extends UnionNode implements IEmptyStructureNode {

	private boolean analyzeDefinitionState;

	@Override
	public boolean isAnalyzeDefinitionState() {
		return analyzeDefinitionState;
	}

	@Override
	public INode getDefinition() {
		for (Dependency d : getDependencies()) {
			if (d instanceof DefinitionDependency && d.getStartArrow().equals(this))
				return d.getEndArrow();
		}
		return null;
	}

	@Override
	public void setAnalyzeDefinitionState(boolean analyzeDefinitionState) {
		this.analyzeDefinitionState = analyzeDefinitionState;
	}

	@Override
	public String getNewType() {
		String name = ((IASTElaboratedTypeSpecifier) getAST().getDeclSpecifier()).getName().toString();
		/*
		 * Ex: union RGB
		 * 
		 * 
		 * Delete union keywork in name
		 */
		name = name.replaceAll("^union\\s*", "");
		return name;
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
		return getNewType();
	}

}
