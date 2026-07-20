package uet.fit.aut.parser.dependency;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTCompoundStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTExpressionStatement;
import uet.fit.aut.parser.obj.AttributeOfStructureVariableNode;
import uet.fit.aut.parser.obj.ClassNode;
import uet.fit.aut.parser.obj.FunctionNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.search.Search;
import uet.fit.aut.search.condition.FunctionNodeCondition;
import uet.fit.aut.util.ASTVisualizer;

import java.util.List;

public class SetterGetterDependencyGeneration extends AbstractDependencyGeneration<AttributeOfStructureVariableNode> {

    @Override
    public void dependencyGeneration(AttributeOfStructureVariableNode var) {
        if (var.getParent() instanceof ClassNode) {
            ClassNode structureNode = (ClassNode) var.getParent();
            List<INode> functionNodes = Search.searchNodes(structureNode, new FunctionNodeCondition());
            for (INode item : functionNodes)
                if (item instanceof FunctionNode) {
                    FunctionNode functionNode = (FunctionNode) item;
                    IASTFunctionDefinition ast = functionNode.getAST();

                    ASTVisitor visitor = new ASTVisitor() {
                        @Override
                        public int visit(IASTExpression expression) {
                            if (expression.getParent() != null && expression.getParent().getParent() != null
                                    && expression.getParent().getParent().getParent() != null
                                    && expression.getParent() instanceof CPPASTExpressionStatement
                                    && expression.getParent().getParent() instanceof ICPPASTCompoundStatement)
                                if (expression instanceof ICPPASTBinaryExpression) {

                                    ICPPASTBinaryExpression assignment = (ICPPASTBinaryExpression) expression;
                                    IASTExpression left = assignment.getOperand1();

                                    String nameLeftVar = ASTVisualizer.toString(left).replace("this->", "");
                                    if (nameLeftVar.equals(var.getNewType())) {
                                        SetterDependency d = new SetterDependency(var, item);
                                        var.getDependencies().add(d);
                                        item.getDependencies().add(d);
                                        return ASTVisitor.PROCESS_ABORT;
                                    }

                                } else if (expression instanceof IASTFunctionCallExpression)
                                    if (expression.getParent().getParent().getParent() instanceof IASTFunctionDefinition) {

                                        IASTNode[] children = expression.getChildren();
                                        String nameCalledFunction = children[0].getRawSignature();
                                        if (nameCalledFunction.equals("strcpy")
                                                || nameCalledFunction.equals("strcpy_s")) {
                                            String des = children[1].getRawSignature();

                                            if (des.equals(var.getNewType())) {
                                                SetterDependency d = new SetterDependency(var, item);
                                                var.getDependencies().add(d);
                                                item.getDependencies().add(d);
                                                return ASTVisitor.PROCESS_ABORT;
                                            }
                                        }
                                    }
                            return ASTVisitor.PROCESS_CONTINUE;
                        }

                        @Override
                        public int visit(IASTStatement stm) {
                            if (stm instanceof IASTReturnStatement) {
                                IASTReturnStatement returnStm = (IASTReturnStatement) stm;
                                IASTInitializerClause returnAST = returnStm.getReturnArgument();

                                if (returnAST != null) {
                                    String returnNameVar = returnAST.getRawSignature();

                                    if (returnNameVar.equals(var.getNewType())) {
                                        GetterDependency d = new GetterDependency(var, item);
                                        var.getDependencies().add(d);
                                        item.getDependencies().add(d);
                                        return ASTVisitor.PROCESS_ABORT;
                                    }
                                }
                            }

                            return ASTVisitor.PROCESS_CONTINUE;
                        }

                    };

                    visitor.shouldVisitStatements = true;
                    visitor.shouldVisitExpressions = true;
                    ast.accept(visitor);
                }
        }
    }
}
