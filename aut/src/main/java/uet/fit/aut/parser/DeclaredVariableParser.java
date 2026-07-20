package uet.fit.aut.parser;

import org.eclipse.cdt.core.dom.ast.*;

import java.util.ArrayList;
import java.util.List;

public class DeclaredVariableParser extends ASTVisitor {
    private List<IASTNode> variables = new ArrayList<>();

    public DeclaredVariableParser() {
        shouldVisitParameterDeclarations = true;
        shouldVisitDeclarations = true;
    }


    @Override
    public int visit(IASTDeclaration declaration) {
        if (declaration instanceof IASTSimpleDeclaration) {
            variables.add(declaration);
        }

        return PROCESS_CONTINUE;
    }

    @Override
    public int visit(IASTParameterDeclaration parameterDeclaration) {
        variables.add(parameterDeclaration);

        return PROCESS_CONTINUE;
    }

    public void setVariables(List<IASTNode> variables) {
        this.variables = variables;
    }

    public List<IASTNode> getVariables() {
        return variables;
    }
}

