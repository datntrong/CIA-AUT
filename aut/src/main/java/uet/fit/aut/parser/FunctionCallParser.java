package uet.fit.aut.parser;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConstructorInitializer;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNewExpression;

import java.util.ArrayList;
import java.util.List;

public class FunctionCallParser extends ASTVisitor {

    private final List<IASTExpression> expressions = new ArrayList<>();

    public List<IASTExpression> getExpressions() {
        return expressions;
    }

    public List<IASTSimpleDeclaration> unexpectedCalledFunctions = new ArrayList<>();

    public List<IASTSimpleDeclaration> getUnexpectedCalledFunctions() {
        return unexpectedCalledFunctions;
    }

    public FunctionCallParser() {
        shouldVisitExpressions = true;
        shouldVisitNames = true;
    }

    @Override
    public int visit(IASTName name) {
        IASTNode parent = name.getParent().getParent();
        if (parent instanceof IASTSimpleDeclaration) {
            IASTSimpleDeclaration declaration = (IASTSimpleDeclaration) parent;
            if (declaration.getDeclarators().length == 1) {
                IASTDeclarator declarator = declaration.getDeclarators()[0];
                if (declarator.getInitializer() instanceof ICPPASTConstructorInitializer) {
                    unexpectedCalledFunctions.add(declaration);
                }
            }
        }

        return PROCESS_CONTINUE;
    }

    @Override
    public int visit(IASTExpression expression)
    {
        if (expression instanceof IASTFunctionCallExpression
                || expression instanceof ICPPASTNewExpression)
            expressions.add(expression);

        return PROCESS_CONTINUE;
    }

}
