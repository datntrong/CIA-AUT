package uet.fit.aut.parser.vardect;

import org.eclipse.cdt.core.dom.ast.*;
import uet.fit.aut.parser.obj.IFunctionNode;
import uet.fit.aut.parser.obj.IVariableNode;
import uet.fit.aut.parser.obj.StaticVariableNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Find all static variables of a function
 * <p>
 * Remain: not detected variable through setter and getter yet
 */
public class StaticVariableDetecter implements IVariableDetecter {
    /**
     * Represent function
     */
    private IFunctionNode function;

    public StaticVariableDetecter(IFunctionNode function) {
        this.function = function;
    }

    @Override
    public List<IVariableNode> findVariables() {
        List<IVariableNode> variableNodes = new ArrayList<>();

        if (function != null) {
            IASTFunctionDefinition ast = function.getAST();

            ASTVisitor visitor = new ASTVisitor() {
                @Override
                public int visit(IASTDeclaration declaration) {
                    if (declaration instanceof IASTSimpleDeclaration) {
                        IASTSimpleDeclaration simpleDeclaration = (IASTSimpleDeclaration) declaration;
                        IASTDeclSpecifier declSpec = simpleDeclaration.getDeclSpecifier();
                        if (declSpec.getStorageClass() == IASTDeclSpecifier.sc_static && !declSpec.isConst()) {
                            List<StaticVariableNode> vars = generateVariable(simpleDeclaration);
                            variableNodes.addAll(vars);
                        }
                    }

                    return PROCESS_CONTINUE;
                }
            };

            visitor.shouldVisitDeclarations = true;

            ast.accept(visitor);
        }

        return variableNodes;
    }

    private List<StaticVariableNode> generateVariable(IASTSimpleDeclaration decList) {
        List<StaticVariableNode> staticVariables = new ArrayList<>();

        for (IASTDeclarator dec : decList.getDeclarators()) {
            IASTSimpleDeclaration decItem = decList.copy(IASTNode.CopyStyle.withLocations);
            int decLength = decItem.getDeclarators().length;
            if (decLength == 0) {
                decItem.addDeclarator(dec);
            } else {
                decItem.getDeclarators()[0] = dec;
                for (int i = 1; i < decLength; i++) {
                    decItem.getDeclarators()[i] = null;
                }
            }

            StaticVariableNode v = new StaticVariableNode();

            v.setAST(decItem);
            v.setParent(function);
            v.setContext(function);

            staticVariables.add(v);
        }

        return staticVariables;
    }

    @Override
    public IFunctionNode getFunction() {
        return function;
    }

    @Override
    public void setFunction(IFunctionNode function) {
        this.function = function;
    }

}
