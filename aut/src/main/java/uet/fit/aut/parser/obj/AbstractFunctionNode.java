package uet.fit.aut.parser.obj;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;
import uet.fit.aut.config.IFunctionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.parser.dependency.*;
import uet.fit.aut.parser.finder.Level;
import uet.fit.aut.parser.finder.VariableSearchingSpace;
import uet.fit.aut.parser.vardect.RelatedExternalVariableDetecter;
import uet.fit.aut.parser.vardect.StaticVariableDetecter;
import uet.fit.aut.search.Search;
import uet.fit.aut.search.condition.GlobalVariableNodeCondition;
import uet.fit.aut.util.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public abstract class AbstractFunctionNode extends CustomASTNode<IASTFunctionDefinition> implements IFunctionNode {

	private final static Logger logger = LoggerFactory.getLogger(AbstractFunctionNode.class);

	// true if the variable node is analyzed size dependency generation before
	// size dependency show relationship between two arguments of a function: an argument is pointer/array, the other one is an
	// integer number representing the size of that pointer/array
	protected boolean sizeDependencyState = false;

	// true if the function node is analyzed function call dependency generation before
	private boolean functionCallDependencyState = false;

	// true if the function node is analyzed global variable dependency generation before
	private boolean globalVariableDependencyState = false;

	// true if the function node is analyzed global variable dependency generation before
	private boolean realParentDependencyState = false;

	private IFunctionConfig functionConfig = null;

//	/**
//	 * Represent the real parent of function. Ex: if function in class is defined
//	 * outside it, then its real parent is this class
//	 */
//	private INode realParent;

	private int visibility;

	protected List<IVariableNode> arguments = new ArrayList<>();
	protected List<StaticVariableNode> staticVariables;

	public void setArguments(List<IVariableNode> arguments) {
		this.arguments = arguments;
	}

	@Override
	public List<IVariableNode> getArguments() {
		if (this.arguments == null || this.arguments.size() == 0) {
			this.arguments = new ArrayList<>();

			for (INode child : getChildren())
				if (child instanceof IVariableNode && !(child instanceof StaticVariableNode)) {
					this.arguments.add((IVariableNode) child);
				}
		}
		return this.arguments;
	}

	public List<StaticVariableNode> getStaticVariables() {
		if (this.staticVariables == null) {
			this.staticVariables = new ArrayList<>();

			for (INode child : getChildren())
				if (child instanceof StaticVariableNode) {
					this.staticVariables.add((StaticVariableNode) child);
				}
		}
		return this.staticVariables;
	}

	@Override
	public List<IVariableNode> getReducedExternalVariables() {
		List<IVariableNode> externalVars = new ArrayList<>();
		for (Dependency d : getDependencies()) {
			if (d instanceof GlobalVariableDependency)
				if (!externalVars.contains(d.getEndArrow()))
					externalVars.add((IVariableNode) d.getEndArrow());
		}

		return externalVars;
	}

	@Override
	public String getFullName() {
		StringBuilder fullName = new StringBuilder(getSingleSimpleName() + "(");
		for (INode var : getArguments())
			fullName.append(var.getNewType()).append(",");
		fullName.append(")");
		fullName = new StringBuilder(fullName.toString().replace(",)", ")"));

		/*
		 * Add prefix of the current name
		 */
		//TODO: full logic
//		String logicPath = getLogicPathFromTopLevel();
//		if (logicPath.length() > 0)
//			return logicPath + SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS + fullName;
//		else
			return fullName.toString();
	}

	/**
	 * Get the name of the function and the types of variables pass into it Ex:
	 * "test(int,int)"
	 */
	@Override
	public String getNewType() {
		StringBuilder output = new StringBuilder(getAST().getDeclarator().getName().toString());
		output.append("(");
		for (IVariableNode parameter : getArguments()) {
			output.append(parameter.getRawType());

//			String defaultVal = parameter.getDefaultValue();
//			if (defaultVal != null)
//				output.append("= ").append(defaultVal);

			output.append(",");
		}
		output.append(")");
		return output.toString()
				.replace(",)", ")")
				.replaceAll("\\s*\\)", "\\)");
	}

	@Override
	public IASTFileLocation getNodeLocation() {
		return getAST().getDeclarator().getFileLocation();
	}

	@Override
	public INode getRealParent() {
		for (Dependency d : this.getDependencies())
			if (d instanceof RealParentDependency && d.getStartArrow().equals(this)) {
				return d.getEndArrow();
			}
		return null;
	}

	@Override
	public String getSimpleName() {
		IASTFunctionDeclarator declarator = getAST().getDeclarator();
		IASTName selectedChild = declarator.getName();

		String simpleName = selectedChild.toString();
		simpleName = simpleName.replace(" ", "");

		return simpleName;
	}

	@Override
	public File getSourceFile() {
		INode sourceCodeFileNode = Utils.getSourcecodeFile(this);
		if (sourceCodeFileNode != null)
			return new File(sourceCodeFileNode.getAbsolutePath());
		else
			return null;
	}

	@Override
	public INode isGetter() {
		for (Dependency d : getDependencies())
			if (d instanceof GetterDependency)
				if (d.getStartArrow() instanceof VariableNode)
					return d.getStartArrow();
				else if (d.getEndArrow() instanceof VariableNode)
					return d.getEndArrow();
		return null;
	}

	@Override
	public INode isSetter() {
		for (Dependency d : getDependencies())
			if (d instanceof SetterDependency)
				if (d.getStartArrow() instanceof VariableNode)
					return d.getStartArrow();
				else if (d.getEndArrow() instanceof VariableNode)
					return d.getEndArrow();
		return null;
	}

	@Override
	public void setAST(IASTFunctionDefinition aST) {
		super.setAST(aST);

		// remove all existing variables node
		for (int i = getChildren().size() - 1; i >= 0; i--) {
			if (getChildren().get(i) instanceof IVariableNode) {
				getChildren().remove(i);
			}
		}

		IASTFunctionDeclarator declarator = getAST().getDeclarator();

		// find arguments
		for (IASTNode child : declarator.getChildren()) {
			if (child instanceof IASTParameterDeclaration) {
				IASTParameterDeclaration astArgument = (IASTParameterDeclaration) child;

				VariableNode argumentNode = new InternalVariableNode();
				argumentNode.setAST(astArgument);
				argumentNode.setParent(this);

				if (!VariableTypeUtils.isVoid(argumentNode.getRealType())) {
					if (!(getChildren().contains(argumentNode)))
						getChildren().add(argumentNode);
				}
			}
		}

		StaticVariableDetecter staticDetecter = new StaticVariableDetecter(this);
		List<IVariableNode> staticVars = staticDetecter.findVariables();
		staticVars.forEach(v -> {
			if (!(getChildren().contains(v)))
				getChildren().add((Node) v);
		});
	}

	@Override
	public void setParent(INode parent) {
		super.setParent(parent);
//		realParent = parent;
	}

	@Override
	public String toString() {
		return getNewType();
	}

	@Override
	public boolean isMethod() {
		INode realParent = getRealParent() == null ? getParent() : getRealParent();
		return realParent instanceof StructureNode;
	}

	@Override
	public boolean isTemplate() {
		INode realParent = getRealParent() == null ? getParent() : getRealParent();

		if (realParent instanceof ClassNode) {
			if (((ClassNode) realParent).isTemplate()) {
				String[] templateParams = TemplateUtils.getTemplateParameters(realParent);

				for (String templateParam : templateParams) {

					if (isUseTemplate(getReturnType(), templateParam))
						return true;

					for (IVariableNode argument : getArguments()) {
						if (isUseTemplate(argument.getRawType(), templateParam))
							return true;
					}
				}
			}
		}

		return AST.getParent() instanceof ICPPASTTemplateDeclaration;
	}

	private boolean isUseTemplate(String type, String templateParam) {
		String simpleType = type.replaceAll("[^\\w]", " ");

		if (simpleType.equals(templateParam))
			return true;

		String[] extended = new String[]{templateParam + " ", " " + templateParam, " " + templateParam + " "};
		for (String extend : extended) {
			if (type.contains(extend))
				return true;
		}

		return false;
	}

	@Override
	public String getSingleSimpleName() {
		String singleSimpleName = getSimpleName();
		if (!singleSimpleName.contains(SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS))
			return singleSimpleName;
		else
			return singleSimpleName
					.substring(singleSimpleName.lastIndexOf(SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS) + 2);
	}

	private VariableNode getReturnNode() {
		for (INode child : getChildren()) {
			if (child instanceof ReturnVariableNode)
				return (VariableNode) child;
		}

		String returnType = getReturnType();

		returnType = VariableTypeUtils.deleteVirtualAndInlineKeyword(returnType);

		VariableNode returnVar = new ReturnVariableNode();
		returnVar.setName(INameRule.RETURN_VARIABLE_NAME_PREFIX);
		returnVar.setRawType(returnType);

		String coreType = returnType.replace(SpecialCharacter.POINTER, "");
		coreType = VariableTypeUtils.deleteStorageClasses(coreType);
		coreType = VariableTypeUtils.deleteStructKeyword(coreType);
		coreType = VariableTypeUtils.deleteUnionKeyword(coreType);
		returnVar.setCoreType(coreType);

		if (getChildren().isEmpty())
			returnVar.setParent(this);
		else {
			if (!(getChildren().get(getChildren().size() - 1) instanceof ReturnVariableNode))
				returnVar.setParent(this);
		}

		return returnVar;
	}


	@Override
	public List<IVariableNode> getExpectedNodeTypes() {
		List<IVariableNode> expectedNodeTypes = getReducedExternalVariables();

		String returnType = getReturnType();
		if (!VariableTypeUtils.isVoid(returnType)) {
			// add a variable representing return value
			VariableNode returnVar = getReturnNode();
			expectedNodeTypes.add(returnVar);
		}

		/*
		 * Add throw variable
		 */
		VariableNode returnVar = new ThrowVariableNode();
		returnVar.setName(INameRule.THROW_VARIABLE_NAME_PREFIX);
		returnVar.setRawType(VariableTypeUtils.THROW);
		returnVar.setCoreType(VariableTypeUtils.THROW);
		returnVar.setParent(this);

		expectedNodeTypes.add(returnVar);

		/*
		 * Add all variables as passing variable
		 */
		for (IVariableNode argument : getPassingVariables())
			if (!expectedNodeTypes.contains(argument))
				expectedNodeTypes.add(argument);

		return expectedNodeTypes;
	}

	@Override
	public List<IVariableNode> getPassingVariables() {
		List<IVariableNode> passingVariables = new ArrayList<>();

		passingVariables.addAll(getArguments());

		passingVariables.addAll(getReducedExternalVariables());

		return passingVariables;
	}

	@Override
	public String getReturnType() {
		IASTDeclSpecifier specifier = getAST().getDeclSpecifier();
		IASTFunctionDeclarator declarator = getAST().getDeclarator();
		return ASTVisualizer.toString(specifier, declarator);
//		IASTFunctionDefinition funDef = getAST();
//
//		IASTDeclSpecifier declSpecifier = getAST().getDeclSpecifier();
//		String returnType = declSpecifier.getRawSignature();
//		if (declSpecifier instanceof IASTNamedTypeSpecifier) {
//			returnType = String.valueOf(((IASTNamedTypeSpecifier) declSpecifier).getName().toCharArray());
//		} else if (declSpecifier instanceof IASTSimpleDeclSpecifier) {
//			returnType = declSpecifier.toString();
//		}
//
//		/*
//		 * Name of function may contain * character. Ex: SinhVien* StrDel2(char s[],int
//		 * k,int h){...} ==> * StrDel2(char s[],int k,int h)
//		 */
//		boolean isReturnReference = false;
//		IASTFunctionDeclarator functionDeclarator = (IASTFunctionDeclarator) funDef.getChildren()[1];
//		IASTNode firstChild = functionDeclarator.getChildren()[0];
//		if (firstChild instanceof IASTPointer)
//			isReturnReference = true;
//		/*
//		 *
//		 */
//		returnType += isReturnReference ? "*" : "";
//
//		return returnType;
	}

	@Override
	public INode clone() {
		IFunctionNode clone = new FunctionNode();
		clone.setAbsolutePath(getAbsolutePath());
		clone.setChildren(getChildren());
		clone.setDependencies(getDependencies());
		clone.setName(getNewType());
		clone.setParent(getParent());
		clone.setFunctionConfig(getFunctionConfig());
		clone.setAST(getAST());
		return clone;
	}

	@Override
	public boolean isStatic() {
		boolean declarationStatic = false;

		for (Dependency d : getDependencies()) {
			if (d instanceof DefinitionDependency) {
				if (d.getStartArrow() instanceof DefinitionFunctionNode) {
					DefinitionFunctionNode declaration = (DefinitionFunctionNode) d.getStartArrow();
					if (declaration.isStatic()) {
						declarationStatic = true;
						break;
					}
				}
			}
		}

		boolean currentStatic = getAST() != null && getAST().getDeclSpecifier()
				.getStorageClass() == IASTDeclSpecifier.sc_static;

		return declarationStatic || currentStatic;
	}

	@Override
	public boolean isVirtual() {
		boolean declarationVirtual = false;

		for (Dependency d : getDependencies()) {
			if (d instanceof DefinitionDependency) {
				if (d.getStartArrow() instanceof DefinitionFunctionNode) {
					DefinitionFunctionNode declaration = (DefinitionFunctionNode) d.getStartArrow();
					if (declaration.isVirtual()) {
						declarationVirtual = true;
						break;
					}
				}
			}
		}

		boolean currentStatic = getAST() != null
				&& getAST().getDeclSpecifier() instanceof ICPPASTDeclSpecifier
				&& ((ICPPASTDeclSpecifier) getAST().getDeclSpecifier()).isVirtual();

		return declarationVirtual || currentStatic;
	}

	@Override
	public List<IVariableNode> getArgumentsAndGlobalVariables() {
		List<IVariableNode> output = new ArrayList<>();
		output.addAll(getArguments());
		output.addAll(new RelatedExternalVariableDetecter(this).findVariables());
		return output;
	}

	@Override
	public void setVisibility(int visibility) {
		this.visibility = visibility;
	}

	@Override
	public int getVisibility() {
		INode realParent = getRealParent() == null ? getParent() : getRealParent();

		if (realParent instanceof StructureNode) {
			for (Dependency d : getDependencies()) {
				if (d instanceof DefinitionDependency) {
					if (d.getStartArrow().getParent().equals(realParent)) {
						if (d.getStartArrow() instanceof ICommonFunctionNode) {
							ICommonFunctionNode definitionNode = (ICommonFunctionNode) d.getStartArrow();
							return definitionNode.getVisibility();
						}
					}
				}
			}

		}

		return visibility;
	}

	//Hoannv

	@Override
	public IFunctionConfig getFunctionConfig() {
		return functionConfig;
	}

	@Override
	public void setFunctionConfig(IFunctionConfig functionConfig) {
		this.functionConfig = functionConfig;
		if (this.functionConfig != null)
			this.functionConfig.setFunctionNode(this);
	}

	public boolean isFunctionCallDependencyState() {
		return functionCallDependencyState;
	}

	public void setFunctionCallDependencyState(boolean functionCallDependencyState) {
		this.functionCallDependencyState = functionCallDependencyState;
	}

	public boolean isGlobalVariableDependencyState() {
		return globalVariableDependencyState;
	}

	public void setGlobalVariableDependencyState(boolean globalVariableDependencyState) {
		this.globalVariableDependencyState = globalVariableDependencyState;
	}

	public boolean isSizeDependencyState() {
		return sizeDependencyState;
	}

	public void setSizeDependencyState(boolean sizeDependencyState) {
		this.sizeDependencyState = sizeDependencyState;
	}

	public boolean isRealParentDependencyState() {
		return realParentDependencyState;
	}

	public void setRealParentDependencyState(boolean realParentDependencyState) {
		this.realParentDependencyState = realParentDependencyState;
	}

	@Override
	public List<IVariableNode> getExternalVariables() {
		List<Level> space = new VariableSearchingSpace(this).getSpaces();
		return Search.searchInSpace(space, new GlobalVariableNodeCondition());
	}

	@Override
	public boolean hasVoidPointerArgument() {
		for (IVariableNode arg : this.getArguments()) {
			if (VariableTypeUtils.isVoidPointer(arg.getRealType())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean hasFunctionPointerArgument() {
		for (IVariableNode arg : this.getArguments()) {
			if (VariableTypeUtils.isFunctionPointer(arg.getRealType())) {
				return true;
			}
		}
		return false;
	}
}
