package uet.fit.aut.parser.dependency;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTArraySubscriptExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTDeclarationStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTExpressionStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.parser.obj.IFunctionNode;
import uet.fit.aut.parser.obj.IVariableNode;
import uet.fit.aut.util.ASTUtils;
import uet.fit.aut.util.Utils;
import uet.fit.aut.util.VariableTypeUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Example: void selectionSort(int arr[], int n), where n is the number of element in arr
 * <p>
 * We need to detect the relationship between n and arr by analyzing the body of function.
 * <p>
 * Example: "for (i = 0; i < n - 1; i++){...arr[i]=1;...}" ==> n is likely to be the number of element in arr
 */
public class SizeOfArrayDepencencyGeneration extends AbstractDependencyGeneration<IFunctionNode> {
    
    private final static Logger logger = LoggerFactory.getLogger(SizeOfArrayDepencencyGeneration.class);
    
    private IFunctionNode function;
    
    public void dependencyGeneration(IFunctionNode root) {
        this.function = root;
        
        if (!this.function.isSizeDependencyState()) {
            List<IVariableNode> numList = getIntegerArguments(this.function);
            List<IVariableNode> pointerOrArrayList = getOneLevelPointerOrOneDimensionalArrayArguments(this.function);
            List<SizeOfArrayOrPointerDependency> dependencies = new ArrayList<>();

            ASTVisitor visitor = new ASTVisitor() {

                @Override
                public int visit(IASTStatement statement) {
                    if (statement instanceof IASTIfStatement) {

                    } else if (statement instanceof IASTForStatement) {
                        // step 1. find the relationship between integer argument and iterator of for loop
                        IASTExpression forCondition = ((IASTForStatement) statement).getConditionExpression();

                        if (forCondition instanceof IASTBinaryExpression) {
                            SizeOfRelationship sizeOfRelationship = findDependenceBetweenIntegerArgumentAndLoopIterator((IASTBinaryExpression) forCondition, numList);

                            if (sizeOfRelationship != null) {
                                // step 2. find the lower bound of iterator
                                findLowerBoundOfIterator((IASTForStatement) statement, sizeOfRelationship);

                                // step 3. find the usage of iterator in the body
                                findDependency(((IASTForStatement) statement).getBody(), sizeOfRelationship, dependencies, pointerOrArrayList);
                            }
                        }

                    } else if (statement instanceof IASTDoStatement) {
                        IASTExpression doCondition = ((IASTDoStatement) statement).getCondition();
                        // TODO:
                    } else if (statement instanceof IASTWhileStatement) {
                        // TODO:
                    }

                    return ASTVisitor.PROCESS_CONTINUE;
                }
            };
            visitor.shouldVisitStatements = true;
            function.getAST().accept(visitor);

            this.function.setSizeDependencyState(true);
        }
    }

    protected void findDependency(IASTStatement body, SizeOfRelationship sizeOfRelationship,
                                  List<SizeOfArrayOrPointerDependency> dependencies, List<IVariableNode> arrayList) {
        List<ICPPASTArraySubscriptExpression> arrayUsed = ASTUtils.getArraySubscriptExpression(body);
        Set<String> analyzed = new HashSet<>();

        for (ICPPASTArraySubscriptExpression item : arrayUsed)
            // if the array element is parsed before
            if (!analyzed.contains(item.getRawSignature())) {
                analyzed.add(item.getRawSignature());
                List<String> indexes = Utils.getIndexOfArray(item.getRawSignature());

                if (indexes.size() == 1) {
                    // one-dimensional array
                    String index = indexes.get(0);
                    if (sizeOfRelationship.getDependentIterator().getRawSignature().equals(index)) {

                        String nameOfArray = Utils.getNameVariable(item.getRawSignature());
                        for (IVariableNode arrayVar : arrayList)
                            if (arrayVar.getName().equals(nameOfArray)) {
                                SizeOfArrayOrPointerDependency d = new SizeOfArrayOrPointerDependency(sizeOfRelationship.getIntegerVar(), arrayVar);

                                if (!sizeOfRelationship.getIntegerVar().getDependencies().contains(d))
                                    sizeOfRelationship.getIntegerVar().getDependencies().add(d);
                                if (!arrayVar.getDependencies().contains(d))
                                    arrayVar.getDependencies().add(d);

                                logger.debug("Found a size dependency: " + d);
                                dependencies.add(d);
                                break;
                            }

                    }
                } else {
                    // two-dimensional arrays or more
                    // do not support
                }
            }
    }

    protected void findLowerBoundOfIterator(IASTForStatement statement, SizeOfRelationship sizeOfRelationship) {
        IASTStatement initializer = ((IASTForStatement) statement).getInitializerStatement();
        if (initializer instanceof CPPASTDeclarationStatement) {
            // for example: "int i = 0"
            IASTNode assignment = initializer.getChildren()[0].getChildren()[1];// get "i = 0"
            String nameOfInitializer = assignment.getChildren()[0].getRawSignature(); // get "i"

            if (nameOfInitializer.equals(sizeOfRelationship.getDependentIterator().getRawSignature())) {
                String startValue = assignment.getChildren()[1].getChildren()[0].getRawSignature(); // get "0"

                if (Utils.toInt(startValue) != Utils.UNDEFINED_TO_INT)
                    sizeOfRelationship.setStartValueOfIterator(Utils.toInt(startValue));
            }

        } else if (initializer instanceof CPPASTExpressionStatement) {
            // for example: "i = 0"
            IASTNode assignment = initializer.getChildren()[0];// get "i = 0"
            String nameOfInitializer = assignment.getChildren()[0].getRawSignature(); // get "i"

            if (nameOfInitializer.equals(sizeOfRelationship.getDependentIterator().getRawSignature())) {
                String startValue = assignment.getChildren()[1].getRawSignature(); // get "0"

                if (Utils.toInt(startValue) != Utils.UNDEFINED_TO_INT)
                    sizeOfRelationship.setStartValueOfIterator(Utils.toInt(startValue));
            }
        }
    }

    protected SizeOfRelationship findDependenceBetweenIntegerArgumentAndLoopIterator(IASTBinaryExpression forCondition, List<IVariableNode> integerArguments) {
        SizeOfRelationship output = null;

//        logger.debug(forCondition.getRawSignature() + " is a binary expression");
        IASTExpression operand1 = ((IASTBinaryExpression) forCondition).getOperand1();
        IASTExpression operand2 = ((IASTBinaryExpression) forCondition).getOperand2();
        IASTExpression dependentIterator = null;
        IVariableNode matchingIntegerArgument = null;
        String comparison = forCondition.getRawSignature().replace(" ", "");

        for (IVariableNode integerArgument : integerArguments) {
            // i < n - 1
            // Case: comparison="n-1>=i"; integerArgument="n"
            if (comparison.equals(integerArgument.getName() + "-1>=" + operand2.getRawSignature())) {
                dependentIterator = operand2;
                matchingIntegerArgument = integerArgument;
                break;
            }
            // Case: comparison="i<=n-1"; integerArgument="n"
            else if (comparison.equals(operand1.getRawSignature() + "<=" + integerArgument.getName() + "-1")) {
                dependentIterator = operand1;
                matchingIntegerArgument = integerArgument;
                break;
            }
            // Case: comparison="n>i"; integerArgument="n"
            else if (comparison.equals(integerArgument.getName() + ">" + operand2.getRawSignature())) {
                dependentIterator = operand2;
                matchingIntegerArgument = integerArgument;
                break;

            }
            // Case: comparison="i<n"; integerArgument="n"
            else if (comparison.equals(operand1.getRawSignature() + "<" + integerArgument.getName())) {
                dependentIterator = operand1;
                matchingIntegerArgument = integerArgument;
                break;
            }
        }
        if (dependentIterator != null && matchingIntegerArgument != null) {
//            logger.debug("dependent variable of " + matchingIntegerArgument.getName() + " is " + dependentIterator.getRawSignature());
            output = new SizeOfRelationship();
            output.setDependentIterator(dependentIterator);
            output.setIntegerVar(matchingIntegerArgument);
        }
        return output;
    }

    protected List<IVariableNode> getIntegerArguments(IFunctionNode function) {
        List<IVariableNode> numList = new ArrayList<>();
        for (IVariableNode argument : function.getArguments()) {
            String type = argument.getRawType();
            if (VariableTypeUtils.isNumBasic(type) && !VariableTypeUtils.isNumBasicFloat(type)) {
                numList.add(argument);
            }
        }
        return numList;
    }

    protected List<IVariableNode> getOneLevelPointerOrOneDimensionalArrayArguments(IFunctionNode function) {
        List<IVariableNode> pointerOrArrayList = new ArrayList<>();
        for (IVariableNode argument : function.getArguments()) {
            String type = argument.getRawType();
            if (VariableTypeUtils.isOneLevel(type) || VariableTypeUtils.isOneDimension(type)) {
                pointerOrArrayList.add(argument);

            }
        }
        return pointerOrArrayList;
    }

    static class SizeOfRelationship {
        IVariableNode integerVar;
        IASTExpression dependentIterator;
        int startValueOfIterator = -9999;
        int endValueOfIterator = -9999;

        public IVariableNode getIntegerVar() {
            return integerVar;
        }

        public void setIntegerVar(IVariableNode integerVar) {
            this.integerVar = integerVar;
        }

        public int getStartValueOfIterator() {
            return startValueOfIterator;
        }

        public void setStartValueOfIterator(int startValueOfIterator) {
            this.startValueOfIterator = startValueOfIterator;
        }

        public int getEndValueOfIterator() {
            return endValueOfIterator;
        }

        public void setEndValueOfIterator(int endValueOfIterator) {
            this.endValueOfIterator = endValueOfIterator;
        }

        public IASTExpression getDependentIterator() {
            return dependentIterator;
        }

        public void setDependentIterator(IASTExpression dependentIterator) {
            this.dependentIterator = dependentIterator;
        }

        @Override
        public String toString() {
            return "argument = " + integerVar.getName() + "; dependent iterator = " + dependentIterator.getRawSignature() + "; start = " + startValueOfIterator + "; end = " + endValueOfIterator;
        }
    }
}
