package uet.fit.aut.parser.obj;

import uet.fit.aut.config.IFunctionConfig;

import java.util.List;

public interface ICommonFunctionNode extends INode {

    /**
     * Get the function configuration of the current function
     *
     * @return
     */
    IFunctionConfig getFunctionConfig();

    /**
     * Set the function configuration
     *
     * @param functionConfig
     */
    void setFunctionConfig(IFunctionConfig functionConfig);

    /**
     * Get arguments. <br/>
     * Ex: "void test(int a,int b)"----------------->arguments = {"int a", "int b"}
     *
     * @return
     */
    List<IVariableNode> getArguments();

    /**
     * Get the passing variables (= arguments + external variables)
     *
     * @return
     */
    List<IVariableNode> getPassingVariables();

    /**
     * Get the return type of the current function
     *
     * @return
     */
    String getReturnType();

    /**
     * Get the simple name of function. VD: function "int* symbolic_execution5(int
     * a, int b){...}" ------------------> "symbolic_execution5"
     *
     * @return
     */
    String getSimpleName();

    /**
     * Return name of function not including namespace, class, struct, etc.. <br/>
     * Ex: "int nsTest0::Student::isAvailable(int
     * id)"------------------"isAvailable"
     *
     * @return
     */
    String getSingleSimpleName();

    /**
     * @return true if the current function is template
     */
    boolean isTemplate();

    /**
     * @return true if the current function is method of structure
     */
    boolean isMethod();


    /**
     * public: 1
     * protected: 2
     * private: 3
     */
    int getVisibility();

    void setVisibility(int v);

    List<IVariableNode> getExternalVariables();

    List<IVariableNode> getArgumentsAndGlobalVariables();

    /**
     * Check whether the function has void* argument or not
     * @return
     */
    boolean hasVoidPointerArgument();

    boolean hasFunctionPointerArgument();

    boolean isStatic();

    boolean isVirtual();
}
