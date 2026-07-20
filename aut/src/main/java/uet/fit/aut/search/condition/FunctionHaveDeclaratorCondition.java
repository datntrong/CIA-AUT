package uet.fit.aut.search.condition;

import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import uet.fit.aut.parser.obj.DefinitionFunctionNode;
import uet.fit.aut.parser.obj.IFunctionNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.search.SearchCondition;

public class FunctionHaveDeclaratorCondition extends SearchCondition {

    private final IASTDeclarator declarator;

    public FunctionHaveDeclaratorCondition(IASTDeclarator declarator) {
        this.declarator = declarator;
    }


    @Override
    public boolean isSatisfiable(INode n) {
        if (n instanceof DefinitionFunctionNode) {
            DefinitionFunctionNode functionNode = (DefinitionFunctionNode) n;
            for (IASTDeclarator dec : functionNode.getAST().getDeclarators()) {
                if (dec.equals(declarator))
                    return true;
            }
        } else if (n instanceof IFunctionNode) {
            IFunctionNode functionNode = (IFunctionNode) n;
            return functionNode.getAST().getDeclarator().equals(declarator);
        }
        return false;
    }
}
