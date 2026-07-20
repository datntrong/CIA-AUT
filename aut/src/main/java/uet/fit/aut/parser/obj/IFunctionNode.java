package uet.fit.aut.parser.obj;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;

import java.util.List;

/**
 * Represent a function in the structure tree
 *
 */
public interface IFunctionNode extends ISourceNavigable, ICommonFunctionNode {

	void setArguments(List<IVariableNode> arguments);

	/**
	 * Get the simple name of function. VD: function "int* symbolic_execution5(int
	 * @return true if the current function return void
	 * a, int b){...}" ------------------> "symbolic_execution5"
	 *
	 * @return
	 */
	String getSimpleName();

	/**			INode clone();
	 * Return name of function not including namespace, class, struct, etc.. <br/>
	 * Ex: "int nsTest0::Student::isAvailable(int			FunctionNormalizer getFnNormalizedASTtoInstrument();
	 * id)"------------------"isAvailable"
	 *			void setFnNormalizedASTtoInstrument(FunctionNormalizer fnNormalizedASTtoInstrument);
	 * @return
	 */
	String getSingleSimpleName();

	/**
	 * Get corresponding abstract syntax tree (AST)
	 *
	 * @return
	 */
	IASTFunctionDefinition getAST();

	/**
	 * Set AST of the current function
	 *
	 * @param ast
	 */
	void setAST(IASTFunctionDefinition ast);

	/**
	 * Get reduced external variables
	 *
	 * @return
	 */
	List<IVariableNode> getReducedExternalVariables();

	/**
	 * Get expected node types
	 *
	 * @return
	 */
	List<IVariableNode> getExpectedNodeTypes();

	/**
	 * Get the real parent of the current function
	 *
	 * @return
	 */
	INode getRealParent();

	List<StaticVariableNode> getStaticVariables();

	/**
	 * Get name of function (in context of class/namespace using "::") and name of
	 * its variables. <br/>
	 * Ex: int* symbolic_execution5(int a, int b){...} ----------->
	 * ClassA::ClassB::symbolic_execution5(a,b);
	 *
	 * @return
	 */
	String getFullName();

	/**
	 * Get the corresponding variable node <br/>
	 * Example, <br/>
	 * <p>
	 * 
	 * <pre>
	 * class Student{
	 * 		private:
	 * 			int age;
	 * 		public:
	 * 			int getAge(){return age;}
	 * }
	 * </pre>
	 * <p>
	 * "getAge()"---------> "int age;"
	 *
	 * @return the variable corresponding to the current function
	 */
	INode isGetter();

	/**
	 * Get the corresponding variable node <br/>
	 * Example, <br/>
	 * <p>
	 * 
	 * <pre>
	 * class Student{
	 * 		private:
	 * 			int age;
	 * 		public:
	 * 			void setAge(int age_){age = age_;}
	 * }
	 * </pre>
	 * <p>
	 * "setAge()"---------> "int age;"
	 *
	 * @return the variable corresponding to the current function
	 */
	INode isSetter();

	INode clone();

	boolean isFunctionCallDependencyState();

	void setFunctionCallDependencyState(boolean functionCallDependencyState);

	boolean isGlobalVariableDependencyState();

	void setGlobalVariableDependencyState(boolean globalVariableDependencyState);

	boolean isSizeDependencyState();

	void setSizeDependencyState(boolean sizeDependencyState);

	boolean isRealParentDependencyState();

	void setRealParentDependencyState(boolean realParentDependencyState);
}
