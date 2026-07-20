package uet.fit.aut.parser.obj;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTVisibilityLabel;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDefinition;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTName;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclSpecifier;
import uet.fit.aut.parser.dependency.TypedefDependency;
import uet.fit.aut.search.Search2;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represent Class, Struct, union, enum
 *
 * @author DucAnh
 */
public abstract class StructureNode extends CustomASTNode<IASTSimpleDeclaration> implements ISourceNavigable {

	protected int visibility = 0;

	public boolean haveTypedef() {
		if (this instanceof StructTypedefNode || this instanceof SpecialStructTypedefNode
				|| this instanceof EnumTypedefNode || this instanceof SpecialEnumTypedefNode
				|| this instanceof UnionTypedefNode || this instanceof SpecialUnionTypedefNode)
			return true;

		return getDependencies().stream().anyMatch(d -> d instanceof TypedefDependency);
	}

	public int getVisibility() {
		return visibility;
	}

	public void setVisibility(int visibility) {
		this.visibility = visibility;
	}

	/**
	 * Get all attributes of the given structure node Ex: <br/>
	 * class A{int a; int b;...} <br/>
	 * -----------------------> return "a", "b"
	 * 
	 * @return
	 */
	public List<IVariableNode> getAttributes() {
		return getChildren().stream()
				.filter(n -> n instanceof AttributeOfStructureVariableNode)
				.map(n -> (IVariableNode) n)
				.collect(Collectors.toList());
	}

	public List<ICommonFunctionNode> getConstructors() {
		ArrayList<ICommonFunctionNode> methods = new ArrayList<>();
		for (INode node : getChildren())
			if (node instanceof ConstructorNode) {
				ConstructorNode f = (ConstructorNode) node;
//				IASTFunctionDefinition ast = f.getAST();
//
//				String decl = ast.getDeclSpecifier().getRawSignature();
//				decl = VariableTypeUtils.deleteStorageClasses(decl);
//				IASTFunctionDeclarator declarator = ast.getDeclarator();
//				IASTNode firstChildOfDeclarator = declarator.getChildren()[0];
//
//				/*
//				  if it is constructor/destructor class/structure
//				 */
//				if (decl.equals("") && firstChildOfDeclarator.getRawSignature().equals(getNewType()))
					methods.add(f);
			} else if (node instanceof ICommonFunctionNode) {
				String functionName = ((ICommonFunctionNode) node).getSingleSimpleName();
				String structureName = getName();
				if (functionName.equals(structureName)) {
					methods.add((ICommonFunctionNode) node);
				}
			}

		if (methods.isEmpty())
			methods.add(generateDefaultConstructor());

		return methods;
	}

	public List<ICommonFunctionNode> getPublicConstructors() {
		List<ICommonFunctionNode> methods = getConstructors();
		methods.removeIf(m -> m.getVisibility() != ICPPASTVisibilityLabel.v_public);
		return methods;
	}

	public List<ICommonFunctionNode> getInstanceMethods() {
		List<ICommonFunctionNode> methods = new ArrayList<>();

		for (INode child : getChildren()) {
			if (child instanceof ICommonFunctionNode
					&& !(child instanceof ConstructorNode)
					&& !(child instanceof DestructorNode)) {
				ICommonFunctionNode method = (ICommonFunctionNode) child;
				if (method.getVisibility() == ICPPASTVisibilityLabel.v_public && method.isStatic()) {
					VariableNode returnNode = null;
					if (method instanceof IFunctionNode) {
						returnNode = Search2.getReturnOf((IFunctionNode) method);
					} else if (method instanceof DefinitionFunctionNode) {
						returnNode = Search2.getReturnOf((DefinitionFunctionNode) method);
					}
					if (returnNode != null) {
						INode correspondingNode = returnNode.getCorrespondingNode();
						if (correspondingNode != null && correspondingNode.equals(this)) {
							methods.add(method);
						}
					}
				}
			}
		}

		return methods;
	}

	public static class DefaultConstructor extends ConstructorNode {

		private String structureName;
		private String constructorName;

		public DefaultConstructor(StructureNode structureNode) {
			structureName = structureNode.getName();
			constructorName = String.format("%s()", structureName);

			genAST();

			setName(constructorName);
			setParent(structureNode);

			String constructorPath = getAbsolutePath() + File.separator + constructorName;
			setAbsolutePath(constructorPath);
		}

		private void genAST() {
			IASTFunctionDefinition definition = new CPPASTFunctionDefinition();
			IASTSimpleDeclSpecifier declSpec = new CPPASTSimpleDeclSpecifier();
			definition.setDeclSpecifier(declSpec);
			IASTFunctionDeclarator declarator = new CPPASTFunctionDeclarator(new CPPASTName(structureName.toCharArray()));
			definition.setDeclarator(declarator);
			setAST(definition);
		}

		@Override
		public String getReturnType() {
			return structureName;
		}

		@Override
		public String getName() {
			return constructorName;
		}

		@Override
		public String toString() {
			return constructorName;
		}
	}

	private ICommonFunctionNode generateDefaultConstructor() {
		DefaultConstructor defaultConstructor = new DefaultConstructor(this);
		getChildren().add(defaultConstructor);
		return defaultConstructor;
	}

	public ArrayList<IVariableNode> getPrivateAttributes() {
		ArrayList<IVariableNode> attributes = new ArrayList<>();
		for (INode node : getChildren())
			if (node instanceof IVariableNode
					&& ((VariableNode) node).getVisibility() == ICPPASTVisibilityLabel.v_private)
				attributes.add((VariableNode) node);
		return attributes;
	}

	public ArrayList<IVariableNode> getPublicAttributes() {
		ArrayList<IVariableNode> attributes = new ArrayList<>();
		for (INode node : getChildren())
			if (node instanceof IVariableNode
					&& ((VariableNode) node).getVisibility() == ICPPASTVisibilityLabel.v_public)
				attributes.add((IVariableNode) node);
		return attributes;
	}

//	public List<INode> getPublicAnonymous() {
//		List<INode> anonymous = new ArrayList<>();
//
//		for (INode child : getChildren()) {
//			if (child instanceof StructureNode && isAnonymousChild((StructureNode) child)
//					&& getAnonymousVisibility((StructureNode) child) == ICPPASTVisibilityLabel.v_public)
//				anonymous.add(child);
//		}
//
//		return anonymous;
//	}
//
//	private boolean isAnonymousChild(StructureNode child) {
//		return child.getName().isEmpty();
//	}
//
//	private int getAnonymousVisibility(StructureNode anonymous) {
//		IASTSimpleDeclaration astStructure = AST;
//		IASTDeclSpecifier declSpec = astStructure.getDeclSpecifier();
//
//		int visibility = this instanceof ClassNode ?
//				ICPPASTVisibilityLabel.v_private : ICPPASTVisibilityLabel.v_public;
//
//		if (declSpec instanceof IASTCompositeTypeSpecifier) {
//			IASTDeclaration[] declarations = ((IASTCompositeTypeSpecifier) declSpec).getDeclarations(true);
//			for (IASTDeclaration declaration : declarations) {
//				if (declaration instanceof ICPPASTVisibilityLabel)
//					visibility = ((ICPPASTVisibilityLabel) declaration).getVisibility();
//				else if (declaration instanceof IASTSimpleDeclaration) {
//					if (declaration.getRawSignature().equals(anonymous.getAST().getRawSignature())) {
//						return visibility;
//					}
//				}
//			}
//		}
//
//		return visibility;
//	}

	@Override
	public void setAST(IASTSimpleDeclaration aST) {
		super.setAST(aST);
	}
}
