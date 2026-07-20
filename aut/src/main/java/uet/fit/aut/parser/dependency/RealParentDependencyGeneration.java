package uet.fit.aut.parser.dependency;

import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNameSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTQualifiedName;
import uet.fit.aut.parser.finder.Level;
import uet.fit.aut.parser.finder.VariableSearchingSpace;
import uet.fit.aut.parser.obj.ExternalVariableNode;
import uet.fit.aut.parser.obj.IEmptyStructureNode;
import uet.fit.aut.parser.obj.IFunctionNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.parser.obj.IVariableNode;
import uet.fit.aut.search.Search;
import uet.fit.aut.search.condition.ClassvsStructvsNamespaceCondition;
import uet.fit.aut.util.SpecialCharacter;

import java.util.List;

/**
 * Get the real parent of a function.
 *
 * I do not consider DefinitionFunctionNode.
 *
 * I just consider ConstructorNode, DescontructorNode, and FunctionNode
 *
 * Real parent is physical parent if the function belongs to file level (not in any class/namespace)
 * For example: source code file x.cpp
 #include "../Person.h"
 int Person::getDoubleWeight(){...} // this function is defined in the class .../Person.h/Person
 *
 *
 * The function Person::getDoubleWeight() has a physical parent: x.cpp
 *
 * The function Person::getDoubleWeight() has a real parent (or logical parent): .../Person.h/Person
 */
public class RealParentDependencyGeneration extends AbstractDependencyGeneration<INode> {

    @Override
    public void dependencyGeneration(INode node) {
        /*
         * Ex: void SinhVien::timSinhVien(int msv){...}
         *
         */
        if (node instanceof ExternalVariableNode && isQualifiedName(node.getName())) {
            reconstructWithAST((IVariableNode) node);
        } else if (node instanceof IFunctionNode && isQualifiedName(((IFunctionNode) node).getSimpleName())) {
                reconstructWithAST((IFunctionNode) node);
        } else {
            RealParentDependency d = new RealParentDependency(node, node.getParent());
            if (!node.getDependencies().contains(d))
                node.getDependencies().add(d);
            if (!node.getParent().getDependencies().contains(d))
                node.getParent().getDependencies().add(d);
        }
    }

    private boolean isQualifiedName(String name) {
        return name.contains(SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS);
    }

    private void reconstructWithAST(IVariableNode variableNode) {
        if (variableNode.getASTDecName() != null) {
            IASTName name = variableNode.getASTDecName().getName();
            handleName(name, variableNode);
        }
    }

    private void reconstructWithAST(IFunctionNode functionNode) {
        IASTName name = functionNode.getAST().getDeclarator().getName();
        handleName(name, functionNode);
    }

    private void handleName(IASTName name, INode node) {
        if (name instanceof ICPPASTQualifiedName) {
            ICPPASTNameSpecifier[] chainNames = ((ICPPASTQualifiedName) name).getQualifier();
            String realParentName = chainNames[chainNames.length - 1].toString();

            List<Level> space = new VariableSearchingSpace(node).generateExtendSpaces();
            List<INode> candidates = Search.searchInSpace(space, new ClassvsStructvsNamespaceCondition(), realParentName);
            final int candidateSize = candidates.size();
            if (candidateSize == 1) {
                INode possibleNode = candidates.get(0);
                addDependency(node, possibleNode);
            } else if (candidateSize > 1) {
                candidates.removeIf(pn -> pn instanceof IEmptyStructureNode);
                if (!candidates.isEmpty()) {
                    INode possibleNode = candidates.get(0);
                    addDependency(node, possibleNode);
                }
            }

            boolean found = false;

            for (Level l : space) {
                for (INode n : l) {
                    List<INode> possibleNodes = Search.searchNodes(n, new ClassvsStructvsNamespaceCondition());
                    for (INode possibleNode : possibleNodes) {
                        if (possibleNode.getName().equals(realParentName)) {
                            RealParentDependency d = new RealParentDependency(node, possibleNode);
                            if (!node.getDependencies().contains(d))
                                node.getDependencies().add(d);
                            if (!possibleNode.getDependencies().contains(d))
                                possibleNode.getDependencies().add(d);
                            found = true;
                            break;
                        }
                    }

                    if (found) break;
                }

                if (found) break;
            }
        }
    }

    private void addDependency(INode node, INode possibleNode) {
        RealParentDependency d = new RealParentDependency(node, possibleNode);
        if (!node.getDependencies().contains(d))
            node.getDependencies().add(d);
        if (!possibleNode.getDependencies().contains(d))
            possibleNode.getDependencies().add(d);
    }
}
