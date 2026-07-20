package uet.fit.aut.parser.vardect;

import org.eclipse.cdt.core.dom.ast.*;
import uet.fit.aut.parser.finder.Level;
import uet.fit.aut.parser.finder.VariableSearchingSpace;
import uet.fit.aut.parser.obj.IFunctionNode;
import uet.fit.aut.parser.obj.IVariableNode;
import uet.fit.aut.logger.IdMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.search.Search;
import uet.fit.aut.search.condition.GlobalVariableNodeCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Find all external variables of a function
 * <p>
 * Remain: not detected variable through setter and getter yet
 */
public class RelatedExternalVariableDetecter extends ASTVisitor implements IVariableDetecter {

    private final static Logger logger = LoggerFactory.getLogger(RelatedExternalVariableDetecter.class);

    /**
     * Represent function
     */
    private IFunctionNode function;

    private final List<IASTName> variableNames = new ArrayList<>();

    private final List<IASTSimpleDeclaration> declarations = new ArrayList<>();

    public RelatedExternalVariableDetecter(IFunctionNode function) {
        this.function = function;
        this.shouldVisitExpressions = true;
        this.shouldVisitDeclarations = true;
        function.getAST().accept(this);
    }

    @Override
    public List<IVariableNode> findVariables() {
        for (IASTSimpleDeclaration declaration : declarations) {
            for (IASTDeclarator declarator : declaration.getDeclarators()) {
                String name = declarator.getName().toString();
                variableNames
                        .removeIf(varName ->
                                varName.toString().equals(name));
            }
        }

        List<IVariableNode> globalVars = getAllGlobalVariables();

        List<IVariableNode> relatedVars = globalVars.stream()
                .filter(this::isUsedInFunction)
                .collect(Collectors.toList());

        if (!relatedVars.isEmpty()) {
            logger.debug(String.format(
                    "Found %d global variables used in %s",
                    relatedVars.size(),
                    IdMapping.getInstance().getOrCreate(function.getName())
            ));
        }

        return relatedVars;
    }

    private boolean isUsedInFunction(IVariableNode v) {
        final String varName = v.getName();
        return variableNames.stream()
                .anyMatch(name -> name.toString().equals(varName));
    }

    private List<IVariableNode> getAllGlobalVariables() {
        List<Level> space = new VariableSearchingSpace(function).getSpaces();
        return Search.searchInSpace(space, new GlobalVariableNodeCondition());
    }

    @Override
    public int visit(IASTExpression expression) {
        if (expression instanceof IASTIdExpression) {
            variableNames.add(((IASTIdExpression) expression).getName());
        }

        return PROCESS_CONTINUE;
    }

    @Override
    public int visit(IASTDeclaration declaration) {
        if (declaration instanceof IASTSimpleDeclaration)
            declarations.add((IASTSimpleDeclaration) declaration);
        return PROCESS_CONTINUE;
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
