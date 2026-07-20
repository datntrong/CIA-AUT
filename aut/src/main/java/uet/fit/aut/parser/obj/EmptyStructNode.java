package uet.fit.aut.parser.obj;

import org.eclipse.cdt.core.dom.ast.IASTElaboratedTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import uet.fit.aut.parser.dependency.DefinitionDependency;
import uet.fit.aut.parser.dependency.Dependency;

import java.io.File;

/**
 * Represent empty struct node
 * <p>
 * Ex:
 * <p>
 * <p>
 * <pre>
 *      struct SinhVien;
 * </pre>
 *
 * @author lamnt
 */
public class EmptyStructNode extends StructNode implements IEmptyStructureNode {

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
        return /* "struct " + */ super.toString();
    }
}
