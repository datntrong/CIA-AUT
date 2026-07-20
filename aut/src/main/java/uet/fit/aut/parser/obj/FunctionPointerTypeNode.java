package uet.fit.aut.parser.obj;

import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;

public class FunctionPointerTypeNode extends CustomASTNode<IASTSimpleDeclaration> implements IFunctionPointerTypeNode {

    private String returnType;

    private String[] argumentTypes;

    private String functionName;

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String[] getArgumentTypes() {
        return argumentTypes;
    }

    public void setArgumentTypes(String[] argumentTypes) {
        this.argumentTypes = argumentTypes;
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }
}
