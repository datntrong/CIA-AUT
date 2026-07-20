package uet.fit.aut.parser.obj;

import org.eclipse.cdt.core.dom.ast.*;
import uet.fit.aut.util.ASTVisualizer;
import uet.fit.aut.util.VariableTypeUtils;

/**
 * Represent single typedef declaration. Ex: typedef char char_t
 *
 * @author DucAnh
 */
public class TypedefDeclaration extends CustomASTNode<IASTSimpleDeclaration> implements ITypedefDeclaration {

    /**
     * Ex1: "typedef int MyIntPtr;"----->"MyIntPtr"
     */
    @Override
    public String getNewType() {
		IASTDeclarator declarator = getAST().getDeclarators()[0];
		String name;
		if (declarator.getNestedDeclarator() != null)
			name = declarator.getNestedDeclarator().getName().toString();
		else
			name = declarator.getName().toString();
        return name;
    }

    /**
     * Ex: "typedef int MyIntPtr;"----->"int"
     */
    @Override
    public String getOldType() {
        String oldName = ASTVisualizer.toString(getAST().getDeclSpecifier());

		/*
         * Ex: "typedef int *MyIntPtr;"
		 */
        if (getAST().getChildren().length >= 2 && getAST().getDeclarators()[0].getChildren().length >= 2) {
            IASTNode firstChild = getAST().getDeclarators()[0].getChildren()[0];

            if (firstChild instanceof IASTPointer) {
				oldName += VariableTypeUtils.POINTER_CHAR /* firstChild.getRawSignature() */;
			} else {
				/*
				 * Ex: "typedef int MyIntPtr[];"
				 */
                IASTNode secondChild = getAST().getDeclarators()[0].getChildren()[1];

                if (secondChild instanceof IASTArrayModifier) {
                	IASTExpression size = ((IASTArrayModifier) secondChild).getConstantExpression();
					oldName += "[" + ASTVisualizer.toString(size) + "]";
				}
            }
        }

		/*
		 * Delete keyword typedef
		 */
        if (getAST().getDeclSpecifier() instanceof IASTElaboratedTypeSpecifier) {
            IASTElaboratedTypeSpecifier decl = (IASTElaboratedTypeSpecifier) getAST().getDeclSpecifier();
			/*
			 * Trường hợp declSpecifier của class là phức tạp, VD "typedef class
			 * thisinh SV"
			 */
            if (decl.getStorageClass() == IASTElaboratedTypeSpecifier.k_struct)
                oldName = oldName.replaceAll("typedef\\s*class\\s*", "");
        }
        return oldName.replaceAll("^typedef\\s*", "");
    }

    @Override
    public String toString() {
        return getNewType();
    }
}
