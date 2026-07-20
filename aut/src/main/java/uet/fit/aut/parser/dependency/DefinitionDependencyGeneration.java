package uet.fit.aut.parser.dependency;

import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.parser.finder.Level;
import uet.fit.aut.parser.finder.VariableSearchingSpace;
import uet.fit.aut.parser.obj.*;
import uet.fit.aut.search.Search;
import uet.fit.aut.search.condition.AbstractFunctionNodeCondition;
import uet.fit.aut.search.condition.CompletedStructureNodeCondition;
import uet.fit.aut.search.condition.FunctionHaveDeclaratorCondition;

import java.io.File;
import java.util.List;
import java.util.Map;

public class DefinitionDependencyGeneration extends AbstractDependencyGeneration<INode> {

    private final static Logger logger = LoggerFactory.getLogger(DefinitionDependencyGeneration.class);

    private Map<String, ISourcecodeFileNode> sourceCache;

    public void dependencyGeneration(INode root) {
        if (root instanceof IFunctionNode && sourceCache != null) {
            handleDefinedFunction((IFunctionNode) root);
        } else if (root instanceof IDeclarationNode) {
            if (!((IDeclarationNode) root).isAnalyzeDefinitionState()) {
                if (root instanceof DefinitionFunctionNode) {
                    handleDeclaredFunction((DefinitionFunctionNode) root);
                } else if (root instanceof IEmptyStructureNode) {
                    handleStructure((IEmptyStructureNode) root);
                }
            }
        }
    }

    private void handleDefinedFunction(IFunctionNode root) {
        IASTFunctionDefinition ast = root.getAST();
        IBinding binding = ast.getDeclarator().getName().resolveBinding();
        if (binding instanceof CPPMethod) {
            IASTDeclarator[] declarations = ((CPPMethod) binding).getDeclarations();
            if (declarations != null) {
                for (IASTDeclarator declarator : declarations) {
                    if (declarator instanceof IASTFunctionDeclarator) {
                        IASTFileLocation fileLocation = declarator.getFileLocation();
                        if (fileLocation != null) {
                            String fileName = fileLocation.getFileName();
                            ISourcecodeFileNode sourceNode = sourceCache.get(fileName);
                            DefinitionFunctionNode functionNode = Search.findFirst(sourceNode, new FunctionHaveDeclaratorCondition(declarator));
                            if (functionNode != null)
                                addDependency(functionNode, root);
                        }
                    }
                }
            }
        }
    }



    private void handleStructure(IEmptyStructureNode structure) {
        String relativePath = File.separator + structure.getName();
        List<Level> space = new VariableSearchingSpace(structure).generateExtendSpaces();
        List<StructureNode> completedStructures = Search.searchInSpace(space, new CompletedStructureNodeCondition(), relativePath);
        if (!completedStructures.isEmpty()) {
            StructureNode referredNode = completedStructures.get(0);
            addDependency(structure, referredNode);
        }
    }

    private void handleDeclaredFunction(DefinitionFunctionNode function) {
        // get function name
        String funcName = function.getSingleSimpleName();

        // find all completed functions having the same name
        List<Level> space = new VariableSearchingSpace(function).generateExtendSpaces();
        List<IFunctionNode> completedFunctions = Search.searchInSpace(space, new AbstractFunctionNodeCondition());
        completedFunctions.removeIf(f -> !(f.getSingleSimpleName().equals(funcName)));

        if (completedFunctions.size() == 1) {
            IFunctionNode referredNode = completedFunctions.get(0);
            addDependency(function, referredNode);
        } else if (completedFunctions.size() > 1) {
            // find all functions having the same parameter number
            int parameters = function.getArguments().size();
            completedFunctions.removeIf(f -> f.getArguments().size() != parameters);
            if (completedFunctions.size() == 1) {
                IFunctionNode referredNode = completedFunctions.get(0);
                addDependency(function, referredNode);
            } else if (completedFunctions.size() > 1) {
                // find all function having the same parameter types
                completedFunctions.removeIf(cores -> !isSameParameters(function, cores));
                if (completedFunctions.size() == 1) {
                    IFunctionNode referredNode = completedFunctions.get(0);
                    addDependency(function, referredNode);
                } else if (completedFunctions.size() > 1) {
                    INode parent = function.getParent();
                    // check parent
                    completedFunctions.removeIf(f -> !parent.equals(f.getRealParent()));
                    if (completedFunctions.size() == 1) {
                        IFunctionNode referredNode = completedFunctions.get(0);
                        addDependency(function, referredNode);
                    }
                }
            }
        }
    }

    private boolean isSameParameters(ICommonFunctionNode function, ICommonFunctionNode cores) {
        for (int i = 0; i < cores.getArguments().size(); i++) {
            IVariableNode coresArg = cores.getArguments().get(i);
            INode coresTypeNode = coresArg.getCorrespondingNode();
            IVariableNode arg = function.getArguments().get(i);
            INode typeNode = arg.getCorrespondingNode();
            if (coresTypeNode != null && typeNode != null && !typeNode.equals(coresTypeNode)) {
                return false;
            }
        }
        return true;
    }

    private void addDependency(IDeclarationNode root, INode referredNode) {
        DefinitionDependency d = new DefinitionDependency(root, referredNode);
        if (!root.getDependencies().contains(d)
                && !referredNode.getDependencies().contains(d)) {
            root.getDependencies().add(d);
            referredNode.getDependencies().add(d);

            root.setAnalyzeDefinitionState(true);

            logger.debug("Found a definition dependency: " + d);
        }
    }

    public void setSourceCache(Map<String, ISourcecodeFileNode> sourceCache) {
        this.sourceCache = sourceCache;
    }
}
