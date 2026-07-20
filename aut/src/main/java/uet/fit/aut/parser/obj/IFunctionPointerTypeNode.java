package uet.fit.aut.parser.obj;

public interface IFunctionPointerTypeNode extends INode {
    String getReturnType();

    String[] getArgumentTypes();

    String getFunctionName();

}
