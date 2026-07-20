package uet.fit.aut.testdata.object;


import uet.fit.aut.instrument.ProjectClone;
import uet.fit.aut.parser.obj.ICommonFunctionNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.parser.obj.InstanceVariableNode;
import uet.fit.aut.search.Search;
import uet.fit.aut.execution.DriverConstant;
import uet.fit.aut.util.SpecialCharacter;
import uet.fit.aut.util.VariableTypeUtils;

public class ConstructorDataNode extends SubprogramNode {

    public ConstructorDataNode(INode fn) {
        super(fn);
    }

    @Override
    public String getRawType() {
        if (super.getRawType() == null) {
            if (getParent() instanceof ValueDataNode) {
                ValueDataNode parent = (ValueDataNode) getParent();

                String type = parent.getCorrespondingVar().getFullType();
                type = VariableTypeUtils.deleteStorageClassesExceptConst(type);
                type = VariableTypeUtils.deleteVirtualAndInlineKeyword(type);
                setRawType(type);

                String realType = parent.getCorrespondingVar().getRealType();
                realType = VariableTypeUtils.deleteStorageClassesExceptConst(realType);
                realType = VariableTypeUtils.deleteVirtualAndInlineKeyword(realType);
                setRealType(realType);
            }
        }

        return super.getRawType();
    }

    @Override
    public void setFunctionNode(INode functionNode) {
        this.functionNode = functionNode;
    }

    public ConstructorDataNode() {
        super();
    }

    @Override
    public String getDisplayName() {
        return getName();
    }

    public String generateInputForParameters(boolean isDeclared) throws Exception {
        return super.getInputForGoogleTest(isDeclared);
    }

    @Override
    public String getInputForGoogleTest(boolean isDeclared) throws Exception {
        String input = "";
        if(!isPassingVariable() || (isPassingVariable() && isDeclared) ){
            input = generateInputForParameters(isDeclared);

            ValueDataNode subclassVar = (ValueDataNode) getParent();
            IDataNode tempClassVar = subclassVar.getParent();

            ValueDataNode classVar = null;
            if (tempClassVar instanceof ValueDataNode) {
                classVar = (ValueDataNode) tempClassVar;
            }

            if (!(classVar instanceof ClassDataNode))
                classVar = subclassVar;

            if (getTestCaseRoot().getFunctionNode().equals(getFunctionNode())) {
                if (classVar.getCorrespondingVar() instanceof InstanceVariableNode) {
                    input += DriverConstant.MARK + "(\"<<PRE-CALLING>>\");";
                }
            }

            input += ProjectClone.generateCallingMark(String.format("%s|%s", functionNode.getAbsolutePath(), getPathFromRoot()));

            String realType = VariableTypeUtils.getFullRawType(subclassVar.getCorrespondingVar());
            String originType = realType;

            if (classVar.isExternal())
                originType = "";

            String argumentInput = getConstructorArgumentsInputForGoogleTest();
            ICommonFunctionNode selectedConstructor = (ICommonFunctionNode) functionNode;
            String functionName = Search.getScopeQualifier(functionNode);
            functionName = functionName.replaceAll("\\(.*\\)", SpecialCharacter.EMPTY);

            String returnType = selectedConstructor.getReturnType();
            if (!VariableTypeUtils.isPointer(returnType))
                functionName = VariableTypeUtils.REFERENCE + functionName;

            String simpleFunctionName = selectedConstructor.getSingleSimpleName();
            String className = subclassVar.getCorrespondingType().getName();

            if (classVar.isInstance()) {
                if (simpleFunctionName.equals(className)) {
                    input += getVirtualName() + " = new " + realType
                            + argumentInput + SpecialCharacter.END_OF_STATEMENT;
                } else {
                    input += getVirtualName() + " = " + functionName
                            + argumentInput + SpecialCharacter.END_OF_STATEMENT;
                }

            }
            // can not use new
            else if (subclassVar.isArrayElement() || subclassVar.isAttribute()) {
                if (simpleFunctionName.equals(className)) {
                    input += getVirtualName() + " = " + realType
                            + argumentInput + SpecialCharacter.END_OF_STATEMENT;
                } else {
                    input += getVirtualName() + " = " + functionName
                            + argumentInput + SpecialCharacter.END_OF_STATEMENT;
                }

            } else if (!(classVar instanceof PointerDataNode)) {
                if (simpleFunctionName.equals(className)) {
                    input += originType + " " + getVirtualName() + " = " + realType
                            + argumentInput + SpecialCharacter.END_OF_STATEMENT;
                } else {
                    input += originType + " " + getVirtualName() + " = "
                            + functionName + argumentInput
                            + SpecialCharacter.END_OF_STATEMENT;
                }

            }
            // which case?
            else {
                if (simpleFunctionName.equals(className)) {
                    input += realType + " " + getVirtualName() + " = new " + getRawType()
                            + argumentInput + SpecialCharacter.END_OF_STATEMENT;
                } else {
                    input += realType + " " + getVirtualName() + " = "
                            + Search.getScopeQualifier(functionNode)
                            + argumentInput + SpecialCharacter.END_OF_STATEMENT;
                }
            }
        }

        return input;
    }

    public String getConstructorArgumentsInputForGoogleTest() {
        StringBuilder input = new StringBuilder();
        input.append("(");

        if (getChildren().size() > 0) {
            for (IDataNode parameter : getChildren()) {
                input.append(parameter.getVirtualName()).append(",");
            }
        }

        input.append(")");

        input = new StringBuilder(input.toString().replace(",)", ")"));

        return input.toString();
    }
}
