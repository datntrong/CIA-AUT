package uet.fit.aut.parser.obj;

import org.eclipse.cdt.core.dom.ast.IASTTypeId;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTAliasDeclaration;
import uet.fit.aut.util.ASTVisualizer;

/**
 * Represent single typedef declaration. Ex: typedef char char_t
 *
 * @author DucAnh
 */
public class AliasDeclaration extends CustomASTNode<ICPPASTAliasDeclaration> implements ITypedefDeclaration {

    /**
     * Ex1: "using Headers = std::multimap<std::string, std::string, detail::ci>;"----->"Headers"
     */
    @Override
    public String getNewType() {
        return getAST().getAlias().toString();
//        return getAST().getChildren()[0].getRawSignature();
    }

    /**
     * Ex: "typedef int MyIntPtr;"----->"int"
     */
    @Override
    public String getOldType() {
        IASTTypeId typeId = getAST().getMappingTypeId();
        return ASTVisualizer.toString(typeId.getDeclSpecifier(), typeId.getAbstractDeclarator());
//        return typeId.getRawSignature();
    }

    @Override
    public String toString() {
        return getNewType();
    }
}
