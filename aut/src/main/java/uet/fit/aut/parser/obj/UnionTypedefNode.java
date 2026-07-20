package uet.fit.aut.parser.obj;

import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;

import java.io.File;

/**
 * Created by DucToan on 13/07/2017.
 */
public class UnionTypedefNode extends UnionNode {
    public UnionTypedefNode() {
        super();
    }

    @Override
    public String getNewType() {
        return getAST().getDeclarators()[0].getName().toString();
    }

    @Override
    public IASTFileLocation getNodeLocation() {
        return ((IASTCompositeTypeSpecifier) getAST().getDeclSpecifier()).getName().getFileLocation();
    }

    @Override
    public File getSourceFile() {
        return new File(getAST().getContainingFilename());
    }

    @Override
    public String toString() {
        return /* "union " + */ super.toString();
    }
}
