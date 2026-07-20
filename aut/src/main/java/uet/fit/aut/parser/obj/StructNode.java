package uet.fit.aut.parser.obj;

import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;

import java.io.File;

/**
 * Represent normal struct node
 * <p>
 * Ex:
 * <p>
 * <p>
 * <pre>
 * struct SinhVien{
 * 			int age;
 * }
 * </pre>
 *
 * @author ducanhnguyen
 */
public class StructNode extends StructOrClassNode {

    @Override
    public String getNewType() {
        return ((IASTCompositeTypeSpecifier) getAST().getDeclSpecifier()).getName().toString();
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
        return /* "struct " + */ super.toString();
    }
}
