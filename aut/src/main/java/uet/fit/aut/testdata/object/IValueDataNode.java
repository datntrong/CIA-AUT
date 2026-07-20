package uet.fit.aut.testdata.object;


import uet.fit.aut.parser.obj.IVariableNode;
import uet.fit.aut.parser.obj.VariableNode;

public interface IValueDataNode extends IDataNode {

    IVariableNode getCorrespondingVar();

    void setCorrespondingVar(VariableNode correspondingVar);

    /**
     * Check whether the node is array item or not
     *
     * @return true if the current node is array item
     */
    boolean isArrayElement();

    boolean isElementInString();

    boolean isSTLListBaseElement();

    boolean isVoidPointerValue();

    /**
     * Check whether the node is attribute or not (element of
     * class/struct/enum/union/etc.)
     */
    boolean isAttribute();

    boolean isStubArgument();

    /**
     * Check whether the node is passing variable or not
     */
    boolean isPassingVariable();

    boolean isExternal();

    void setExternal(boolean _external);

    boolean isInConstructor();

    String getAssertion();

    String getRawType();

    void setRawType(String rawType);

    String getRealType();

    void setRealType(String realType);
}
