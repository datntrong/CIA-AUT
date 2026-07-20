package uet.fit.aut.parser;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.IASTNode.CopyStyle;
import org.eclipse.cdt.core.dom.ast.cpp.*;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclaration;
import org.slf4j.helpers.NOPLogger;
import org.slf4j.Logger;

import uet.fit.aut.logger.IdMapping;
import uet.fit.aut.parser.obj.*;
import uet.fit.aut.util.ASTVisualizer;
import uet.fit.aut.util.SpecialCharacter;
import uet.fit.aut.util.TemplateUtils;
import uet.fit.aut.util.VariableTypeUtils;

import java.io.File;
import java.util.*;

public class SourcecodeFileExpander implements ISourcecodeFileParser {

//	private final static Logger logger = LoggerFactory.getLogger(SourcecodeFileExpander.class);
	private final static Logger logger = new NOPLogger() {};

	protected INode root;

	public SourcecodeFileExpander() {
	}

	public void expand(IASTNode ast)  {
		logger.info("Parsing " + IdMapping.getInstance().getOrCreate(root.getName()) + " to AST");

		logger.info("Traversing abstract syntax tree of " + IdMapping.getInstance().getOrCreate(root.getName()));
		CustomCppStack stackNodes = new CustomCppStack();
		stackNodes.push(root);

		VisibilityStack visibilityStack = new VisibilityStack();
		Visibility visibility = new Visibility(ICPPASTVisibilityLabel.v_public, ast.getParent());
		visibilityStack.push(visibility);

		ASTVisitor visitor = new ASTVisitor() {
			@Override
			public int leave(IASTDeclaration declaration) {
				stackNodes.pop();

                /*
                  Nếu thoát khỏi class/struct thì xóa scope
                 */
				int declarationType = getTypeOfAstDeclaration(declaration);

				switch (declarationType) {
					case IS_CLASS_DECLARATION:
					case IS_STRUCT_DECLARATION:
					case IS_STRUCT_TYPEDEF_DECLARATION:
					case IS_UNION:
					case IS_UNION_TYPEDEF_DECLARATION:

						while (!visibilityStack.isEmpty()) {
							Visibility v = visibilityStack.peek();
							visibilityStack.pop();
							if (v.ast() == declaration) {
								break;
							}
						}

						break;
				}

				return ASTVisitor.PROCESS_CONTINUE;
			}

			@Override
			public int leave(ICPPASTNamespaceDefinition namespaceDefinition) {
				stackNodes.pop();
				return ASTVisitor.PROCESS_CONTINUE;
			}

			@Override
			public int visit(IASTDeclaration declaration) {
//				logger.debug("Visiting " + declaration.getClass().getSimpleName() + " at " + declaration.getFileLocation());

                /*
                  IASTDeclaration đại diện một khai bảo trong mã nguồn
                 */
				Node declarationNode = new TemporaryNode("tmpNode");

				int typeOfDeclaration = getTypeOfAstDeclaration(declaration);

				switch (typeOfDeclaration) {
					case IS_FUNCTION_POINTER_TYPEDEF_DECLARATION: {
						declarationNode = new FunctionPointerTypedefDeclaration();
						((FunctionPointerTypedefDeclaration) declarationNode)
								.setAST((IASTSimpleDeclaration) declaration);
						break;
					}

					case IS_LINKAGE_SPECIFICATION: {
						declarationNode = new LinkageSpecificationDeclaration();
						((LinkageSpecificationDeclaration) declarationNode)
								.setAST((ICPPASTLinkageSpecification) declaration);
						break;
					}

					case IS_ALIAS_DECLARATION:
						declarationNode = new AliasDeclaration();

						((AliasDeclaration) declarationNode)
								.setAST((ICPPASTAliasDeclaration) declaration);

						break;

					case IS_FUNCTION_DECLARATION:
						if (declaration instanceof ICPPASTTemplateDeclaration)
							break;

						declarationNode = new FunctionNode();
						((FunctionNode) declarationNode)
								.setAST((IASTFunctionDefinition) declaration);
						((FunctionNode) declarationNode)
								.setVisibility(visibilityStack.peek().value());
						break;

					case IS_FUNCTION_AS_VARIABLE_DECLARATION:
						declarationNode = new DefinitionFunctionNode();
						((DefinitionFunctionNode) declarationNode)
								.setAST((CPPASTSimpleDeclaration) declaration);
						((DefinitionFunctionNode) declarationNode)
								.setVisibility(visibilityStack.peek().value());
						break;

					case IS_CONSTRUCTOR_DECLARATION:
						declarationNode = new ConstructorNode();
						((ConstructorNode) declarationNode)
								.setAST((IASTFunctionDefinition) declaration);
						((ConstructorNode) declarationNode)
								.setVisibility(visibilityStack.peek().value());
						break;

					case IS_DESTRUCTOR_DECLARATION:
						declarationNode = new DestructorNode();
						((DestructorNode) declarationNode)
								.setAST((IASTFunctionDefinition) declaration);
						((DestructorNode) declarationNode)
								.setVisibility(visibilityStack.peek().value());
						break;

					case IS_TEMPLATE_DECLARATION:
						declarationNode = new TemplateWrapper((ICPPASTTemplateDeclaration) declaration);
						break;

					case IS_STRUCT_DECLARATION: {
						declarationNode = new StructNode();
						((StructNode) declarationNode)
								.setAST((IASTSimpleDeclaration) declaration);
						((StructureNode) declarationNode)
								.setVisibility(visibilityStack.peek().value());

						Visibility v = new Visibility(ICPPASTVisibilityLabel.v_public, declaration);
						visibilityStack.push(v);
						break;
					}

					case IS_CLASS_DECLARATION: {
						declarationNode = new ClassNode();
						((ClassNode) declarationNode)
								.setAST((IASTSimpleDeclaration) declaration);
						((StructureNode) declarationNode)
								.setVisibility(visibilityStack.peek().value());

						Visibility v = new Visibility(ICPPASTVisibilityLabel.v_private, declaration);
						visibilityStack.push(v);
						break;
					}

					case IS_EMPTY_STRUCT_DECLARATION:
						declarationNode = new EmptyStructNode();
						((EmptyStructNode) declarationNode)
								.setAST((IASTSimpleDeclaration) declaration);
						((StructureNode) declarationNode)
								.setVisibility(visibilityStack.peek().value());
						break;

					case IS_EMPTY_ENUM_DECLARATION:
						declarationNode = new EmptyEnumNode();
						((EmptyEnumNode) declarationNode)
								.setAST((IASTSimpleDeclaration) declaration);
						((StructureNode) declarationNode)
								.setVisibility(visibilityStack.peek().value());
						break;

					case IS_EMPTY_CLASS_DECLARATION:
						declarationNode = new EmptyClassNode();
						((EmptyClassNode) declarationNode)
								.setAST((IASTSimpleDeclaration) declaration);
						((StructureNode) declarationNode)
								.setVisibility(visibilityStack.peek().value());
						break;

					case IS_EMPTY_UNION_DECLARATION:
						declarationNode = new EmptyUnionNode();
						((EmptyUnionNode) declarationNode)
								.setAST((IASTSimpleDeclaration) declaration);
						((StructureNode) declarationNode)
								.setVisibility(visibilityStack.peek().value());
						break;

					case IS_VARIABLE_DECLARATION: {
						int visibility = visibilityStack.peek().value();
						declarationNode = addVariableNode((IASTSimpleDeclaration) declaration, stackNodes, declarationNode, visibility);
						break;
					}

					case IS_PRIMITIVE_TYPEDEF_DECLARATION: {
						IASTSimpleDeclaration decList = (IASTSimpleDeclaration) declaration;
						IASTDeclSpecifier type = decList.getDeclSpecifier();
						INodeFactory fac = ((IASTTranslationUnit) ast.getParent()).getASTNodeFactory();

						for (IASTDeclarator dec : decList.getDeclarators()) {
							IASTSimpleDeclaration decItem = fac
									.newSimpleDeclaration(type
											.copy(CopyStyle.withLocations));
							decItem.addDeclarator(dec.copy(CopyStyle.withLocations));

							stackNodes.push(declarationNode);
							stackNodes.pop();

							TypedefDeclaration td = new PrimitiveTypedefDeclaration();
							td.setAST(decItem);
							declarationNode = td;
						}

						break;
					}

					case IS_STRUCT_TYPEDEF_DECLARATION: {
						/*
						 * Ex1: typedef struct MyStruct4{ int x; } MyStruct5;
						 *
						 * Ex2: typedef struct { int x; } MyStruct5;
						 */
						declarationNode = new StructTypedefNode();
						((StructTypedefNode) declarationNode)
								.setAST((IASTSimpleDeclaration) declaration);
						((StructureNode) declarationNode)
								.setVisibility(visibilityStack.peek().value());

						Visibility v = new Visibility(ICPPASTVisibilityLabel.v_public, declaration);
						visibilityStack.push(v);
						break;
					}

					case IS_PROTECTED_LABEL: {
						Visibility v = new Visibility(ICPPASTVisibilityLabel.v_protected, declaration);
						visibilityStack.push(v);
						break;
					}

					case IS_PRIVATE_LABEL: {
						Visibility v = new Visibility(ICPPASTVisibilityLabel.v_private, declaration);
						visibilityStack.push(v);
						break;
					}

					case IS_PUBLIC_LABEL: {
						Visibility v = new Visibility(ICPPASTVisibilityLabel.v_public, declaration);
						visibilityStack.push(v);
						break;
					}

					case IS_ENUM:
						declarationNode = new EnumNode();
						((EnumNode) declarationNode)
								.setAST((IASTSimpleDeclaration) declaration);
						((StructureNode) declarationNode)
								.setVisibility(visibilityStack.peek().value());
						break;

					case IS_ENUM_TYPEDEF_DECLARATION:
						declarationNode = new EnumTypedefNode();
						((EnumTypedefNode) declarationNode)
								.setAST((IASTSimpleDeclaration) declaration);
						((StructureNode) declarationNode)
								.setVisibility(visibilityStack.peek().value());
						break;

					case IS_UNION: {
						declarationNode = new UnionNode();
						((UnionNode) declarationNode)
								.setAST((IASTSimpleDeclaration) declaration);
						((StructureNode) declarationNode)
								.setVisibility(visibilityStack.peek().value());

						Visibility v = new Visibility(ICPPASTVisibilityLabel.v_public, declaration);
						visibilityStack.push(v);
						break;
					}

					case IS_UNION_TYPEDEF_DECLARATION: {
						declarationNode = new UnionTypedefNode();
						((UnionTypedefNode) declarationNode).setAST((IASTSimpleDeclaration) declaration);
						((StructureNode) declarationNode)
								.setVisibility(visibilityStack.peek().value());

						Visibility v = new Visibility(ICPPASTVisibilityLabel.v_public, declaration);
						visibilityStack.push(v);
						break;
					}

					case IS_UNSPECIFIED_DECLARATION:
					default: {
						declarationNode = new UnspecifiedDeclaration();
						((UnspecifiedDeclaration) declarationNode).setAST(declaration);
						break;
					}
				}

				stackNodes.push(declarationNode);

//				logger.debug("Found " + declarationNode.getClass().getSimpleName());

				if (typeOfDeclaration == IS_FUNCTION_DECLARATION) {
					stackNodes.pop();
					return ASTVisitor.PROCESS_SKIP;
				}

				return ASTVisitor.PROCESS_CONTINUE;
			}

			@Override
			public int visit(ICPPASTNamespaceDefinition namespaceDefinition) {
//				logger.debug("Visiting " + namespaceDefinition.getClass().getSimpleName() + " at " + namespaceDefinition.getFileLocation());

				NamespaceNode namespaceNode = new NamespaceNode();
				namespaceNode.setAST(namespaceDefinition);
				stackNodes.push(namespaceNode);

//				logger.debug("Found " + namespaceNode.getClass().getSimpleName());

				return ASTVisitor.PROCESS_CONTINUE;
			}

		};
		visitor.shouldVisitDeclarations = true;
		visitor.shouldVisitNamespaces = true;

		ast.accept(visitor);

		INode root = stackNodes.rootOfStack;

//		logger.debug("Finish traversing abstract syntax tree of " + IdMapping.getInstance().getOrCreate(file.getName()));

//		addIncludeHeaderNodes(getHeader(translationUnit), root);

//      createSpecialNode(root);
//		parseMacros(root);

		logger.info("Expanding " + IdMapping.getInstance().getOrCreate(this.root.getName()) + " successfully");
	}

	private Node addVariableNode(IASTSimpleDeclaration decList, CustomCppStack stackNodes,
			Node declarationNode, int visibility) {
		for (IASTDeclarator dec : decList.getDeclarators()) {
			IASTSimpleDeclaration decItem = decList.copy(IASTNode.CopyStyle.withLocations);
			int decLength = decList.getDeclarators().length;
			if (decLength == 0) {
				decItem.addDeclarator(dec);
			} else {
				decItem.getDeclarators()[0] = dec;
				for (int i = 1; i < decLength; i++) {
					decItem.getDeclarators()[i] = null;
				}
			}

			stackNodes.push(declarationNode);
			stackNodes.pop();

			// Note: We have two types of variables known as internal variable, and external variable.
			// Internal variable are passed into the function, e.g., void test(int a, int b) -----> a, b: internal variable
			// All variables declared outside functions are considered as external variables.
			// Because we only parse down to method level, so all variables discovered in this process belong to kind of external variables.
			VariableNode var = null;
			if (stackNodes.peek() instanceof StructureNode) {
				var = new AttributeOfStructureVariableNode();
			} else if (stackNodes.peek() instanceof ISourcecodeFileNode
					|| stackNodes.peek() instanceof NamespaceNode) {
				var = new ExternalVariableNode();
			}

			if (var != null) {
				var.setAST(decItem);
				var.setVisibility(visibility);
				declarationNode = var;
			}
		}

		return declarationNode;
	}

	/**
	 * Lấy danh sách include của một tệp
	 */
	private List<IASTPreprocessorIncludeStatement> getHeader(IASTTranslationUnit u) {
		List<IASTPreprocessorIncludeStatement> includes = new ArrayList<>();
		for (IASTPreprocessorIncludeStatement includeDirective : u.getIncludeDirectives())
			// if the include header is active by macro
			if (includeDirective.isActive() && includeDirective.getNodeLocations().length > 0) {
				includes.add(includeDirective);
			}
		return includes;
	}

	/**
	 * Lấy kiểu AST Node
	 *
	 * @param astNode
	 * @return
	 */
	private int getTypeOfAstDeclaration(IASTDeclaration astNode) {
		if (astNode instanceof ICPPASTLinkageSpecification) {
			return IS_LINKAGE_SPECIFICATION;

		} else if (astNode instanceof ICPPASTAliasDeclaration) {
			return IS_ALIAS_DECLARATION;
		} else
			// Câu lệnh rỗng
			if (astNode instanceof IASTNullStatement)
				return IS_UNSPECIFIED_DECLARATION;
			else
				/*
				 * Ex: enum Color { RED, GREEN, BLUE };
				 */
				if (astNode.getChildren().length >= 1
						&& astNode.getChildren()[0] instanceof ICPPASTEnumerationSpecifier
						//TODO: LAMNT FIX
//						&& astNode.getChildren()[0].getRawSignature().contains(ENUM_SYMBOL)
				) {
					ICPPASTEnumerationSpecifier specifier = (ICPPASTEnumerationSpecifier) astNode.getChildren()[0];
					if (specifier.getStorageClass() == IASTDeclSpecifier.sc_typedef)
						return IS_ENUM_TYPEDEF_DECLARATION;
					else
						return IS_ENUM;
				} else
					/*
					 * Ex: union RGBA{ int color; int aliasColor;}
					 */
					if (astNode.getChildren().length >= 1
							&& astNode.getChildren()[0] instanceof IASTCompositeTypeSpecifier
							//TODO: LAMNT FIX
							&& ((IASTCompositeTypeSpecifier) astNode.getChildren()[0]).getKey() == IASTCompositeTypeSpecifier.k_union
//							&& astNode.getChildren()[0].getRawSignature().contains(UNION_SYMBOL)
					) {
						IASTCompositeTypeSpecifier specifier = (IASTCompositeTypeSpecifier) astNode.getChildren()[0];
						if (specifier.getStorageClass() == IASTDeclSpecifier.sc_typedef)
							return IS_UNION_TYPEDEF_DECLARATION;
						else
							return IS_UNION;
					} else
						/*
						 *
						 * Nếu node là public/private/protected
						 */
						if (astNode instanceof ICPPASTVisibilityLabel)
							switch (((ICPPASTVisibilityLabel) astNode).getVisibility()) {
								case ICPPASTVisibilityLabel.v_private:
									return IS_PRIVATE_LABEL;
								case ICPPASTVisibilityLabel.v_protected:
									return IS_PROTECTED_LABEL;
								case ICPPASTVisibilityLabel.v_public:
									return IS_PUBLIC_LABEL;
							}
						else if (astNode instanceof IASTFunctionDefinition) {
							if (((IASTFunctionDefinition) astNode).getBody() != null) {
//							if (astNode.getRawSignature().contains(
//									FUNCTION_BODY_SIGNAL)) {
								if (isConstructor((IASTFunctionDefinition) astNode))
									return IS_CONSTRUCTOR_DECLARATION;
								else if (isDestructor((IASTFunctionDefinition) astNode))
									return IS_DESTRUCTOR_DECLARATION;
								else
									return IS_FUNCTION_DECLARATION;
							}
							// TODO: Lamnt fix
							else if (isConstructor((IASTFunctionDefinition) astNode))
								return IS_CONSTRUCTOR_DECLARATION;
							else if (isDestructor((IASTFunctionDefinition) astNode))
								return IS_DESTRUCTOR_DECLARATION;
							else
								return IS_UNSPECIFIED_DECLARATION;
						} else if (astNode instanceof ICPPASTTemplateDeclaration)
							return IS_TEMPLATE_DECLARATION;
						else if (astNode instanceof IASTSimpleDeclaration) {
							// special case: "struct Node *quickSortRecur(struct Node *head, struct Node *end);" put in a namespace
							// it is the definition of a function, not a variable declaration
							for (IASTDeclarator declarator : ((IASTSimpleDeclaration) astNode).getDeclarators()) {
								if (declarator instanceof IASTFunctionDeclarator) {
									if (declarator.getNestedDeclarator() != null)
//									if (astNode.getRawSignature().startsWith(TYPEDEF_SYMBOL))
										// typedef int (*ListCompareFunc)(ListValue value1, ListValue value2);
										return IS_FUNCTION_POINTER_TYPEDEF_DECLARATION;
									else
										// *quickSortRecur(struct Node *head, struct Node *end)
										return IS_FUNCTION_AS_VARIABLE_DECLARATION;
								}
							}

							/*
							 * IASTSimpleDeclaration đại diện câu lệnh khai báo biến, struct,
							 * class, enum, union
							 */
							IASTDeclSpecifier declSpecifier = ((IASTSimpleDeclaration) astNode)
									.getDeclSpecifier();

							IASTDeclarator[] declarators = ((IASTSimpleDeclaration) astNode)
									.getDeclarators();
							/*
							 * IASTCompositeTypeSpecifier đại diện cho cấu trúc chứa khai báo
							 * nhiều thành phần con bên trong VD: struct, class, union.
							 */
							if (declSpecifier instanceof IASTCompositeTypeSpecifier) {
								switch (((IASTCompositeTypeSpecifier) declSpecifier).getKey()) {

									case IASTCompositeTypeSpecifier.k_struct:
										/*
										 * Ex: typedef struct { int x; } MyStruct1;
										 */
										if (declSpecifier.getStorageClass() == IASTDeclSpecifier.sc_typedef)
//										if (astNode.getRawSignature().startsWith("typedef "))
											return IS_STRUCT_TYPEDEF_DECLARATION;
										else
											return IS_STRUCT_DECLARATION;
									case ICPPASTCompositeTypeSpecifier.k_class:
										return IS_CLASS_DECLARATION;
								}
							}
							/* Lam fix bug with attribute contains struct keyword
							 * TODO: union
							 */
							else if (declSpecifier instanceof IASTElaboratedTypeSpecifier) {
								if (declSpecifier.getStorageClass() == IASTDeclSpecifier.sc_typedef)
									return IS_PRIMITIVE_TYPEDEF_DECLARATION;
								else if (declarators.length == 0) {
									int kind = ((IASTElaboratedTypeSpecifier) declSpecifier).getKind();
									switch (kind) {
										case IASTElaboratedTypeSpecifier.k_enum:
											return IS_EMPTY_ENUM_DECLARATION;

										case IASTElaboratedTypeSpecifier.k_struct:
											return IS_EMPTY_STRUCT_DECLARATION;

										case IASTElaboratedTypeSpecifier.k_union:
											return IS_EMPTY_UNION_DECLARATION;

										default:
											return IS_EMPTY_CLASS_DECLARATION;
									}
								} else
									return IS_VARIABLE_DECLARATION;
							} else
								/*
								 * IASTSimpleDeclSpecifier tương ứng với biến kiểu cơ bản như int,
								 * float, double, v.v.
								 */
								if (declSpecifier instanceof IASTSimpleDeclSpecifier
										|| declSpecifier instanceof IASTNamedTypeSpecifier)
									/*
									 * CPPASTNamedTypeSpecifier tương ứng với biến tự định nghĩa. Ví dụ
									 * như kiểu DEPT trong khai báo DEPT department;
									 */ {
									if (declSpecifier.getStorageClass() == IASTDeclSpecifier.sc_typedef)
										return IS_PRIMITIVE_TYPEDEF_DECLARATION;
//                                    else if (!astNode.getRawSignature().contains(
//                                            METHOD_SIGNAL))
//                                        return IS_VARIABLE_DECLARATION;
//                                    else
//                                        return IS_FUNCTION_AS_VARIABLE_DECLARATION;
										//TODO: Lam fix
									else if (declarators.length > 0) {
										if (declarators[0] instanceof IASTFunctionDeclarator)
											return IS_FUNCTION_AS_VARIABLE_DECLARATION;
										else
											return IS_VARIABLE_DECLARATION;
									}
								} else if (declSpecifier instanceof ICPPASTEnumerationSpecifier) {
									/*
									 * Ex: typedef enum {
									 * 	RB_TREE_NODE_RED,
									 * 	RB_TREE_NODE_BLACK,
									 * } RBTreeNodeColor;
									 */
									ICPPASTEnumerationSpecifier decl = (ICPPASTEnumerationSpecifier) declSpecifier;
									if (decl.getStorageClass() == IASTDeclSpecifier.sc_typedef)
										return IS_PRIMITIVE_TYPEDEF_DECLARATION;
								}
						}

		return IS_UNSPECIFIED_DECLARATION;
	}

	private boolean isConstructor(IASTFunctionDefinition ast) {
		String decl = ASTVisualizer.toString(ast.getDeclSpecifier());
		decl = VariableTypeUtils.deleteStorageClasses(decl);
		decl = VariableTypeUtils.deleteVirtualAndInlineKeyword(decl);
		IASTFunctionDeclarator declarator = ast.getDeclarator();
		IASTNode firstChildOfDeclarator = declarator.getChildren()[0];
		String name = declarator.getName().toString();

		/*
		 * Nếu hàm không có kiểu trả về + tên hàm giống tên class/structure
		 */
		return decl.isEmpty()
//				&& !firstChildOfDeclarator.getRawSignature().startsWith(SpecialCharacter.STRUCTURE_DESTRUCTOR)
//				&& !firstChildOfDeclarator.getRawSignature().contains(SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS + SpecialCharacter.STRUCTURE_DESTRUCTOR);
				&& !name.startsWith(SpecialCharacter.STRUCTURE_DESTRUCTOR)
				&& !name.contains(SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS + SpecialCharacter.STRUCTURE_DESTRUCTOR);
	}

	private boolean isDestructor(IASTFunctionDefinition ast) {
		String decl = ASTVisualizer.toString(ast.getDeclSpecifier());
		decl = VariableTypeUtils.deleteStorageClasses(decl);
		decl = VariableTypeUtils.deleteVirtualAndInlineKeyword(decl);
		IASTFunctionDeclarator declarator = ast.getDeclarator();
		IASTNode firstChildOfDeclarator = declarator.getChildren()[0];
		String name = declarator.getName().toString();

		/*
		 * Nếu hàm không có kiểu trả về + tên hàm giống tên class/structure
		 */
		return decl.isEmpty()
//				&& (firstChildOfDeclarator.getRawSignature().startsWith(SpecialCharacter.STRUCTURE_DESTRUCTOR)
//				|| firstChildOfDeclarator.getRawSignature().contains(SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS + SpecialCharacter.STRUCTURE_DESTRUCTOR));
				&& (name.startsWith(SpecialCharacter.STRUCTURE_DESTRUCTOR)
				|| name.contains(SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS + SpecialCharacter.STRUCTURE_DESTRUCTOR));
	}

	public void setRoot(INode root) {
		this.root = root;
	}

	/**
	 * Chúng ta cần một stack lưu các node đã thăm, với mục đích tạo quan hệ cha
	 * - con giữa các node
	 *
	 * @author DucAnh
	 */
	private static class CustomCppStack extends Stack<INode> {

		private static final long serialVersionUID = 1L;
		private INode rootOfStack;

		/**
		 * Khi thêm một node mới vào stack, ta tạo luôn quan hệ cha - con với
		 * node trước đó. Ngoài ra, nếu node đó là node đầu tiên thêm vào stack,
		 * hiển nhiên node đó là root
		 */
		@Override
		public INode push(INode item) {
			if (size() == 0)
				rootOfStack = item;
			else if (item instanceof TemporaryNode || item instanceof TemplateWrapper) {
				// Khong xem xet virtual (temporary) Node
			} else {
				INode peek = peek();
				if (peek instanceof TemplateWrapper)
					peek = elementAt(size() - 2);

				synchronized (peek.getChildren()) {
					peek.getChildren().add(item);
					item.setParent(peek);
				}

				if (!peek.getAbsolutePath().endsWith(File.separator))
					item.setAbsolutePath(peek.getAbsolutePath() + File.separator + item.getNewType());
				else
					item.setAbsolutePath(peek.getAbsolutePath() + item.getNewType());
			}

			return super.push(item);
		}
	}

	/**
	 * Temporary node (will be remove after)
	 */
	private static class TemporaryNode extends Node {
		public TemporaryNode(String name) {
			setName(name);
		}
	}

	/**
	 * Template wrapper (will be removed after)
	 */
	private static class TemplateWrapper extends Node {
		public TemplateWrapper(ICPPASTTemplateDeclaration ast) {
			StringBuilder b = new StringBuilder(TemplateUtils.OPEN_TEMPLATE_ARG);
			ICPPASTTemplateParameter[] parameters = ast.getTemplateParameters();
			for (int i = 0; i < parameters.length; i++) {
				b.append(parameters[i].getRawSignature());
				if (i < parameters.length - 1) {
					b.append(SpecialCharacter.COMMA);
				}
			}
			b.append(TemplateUtils.CLOSE_TEMPLATE_ARG);
			setName(b.toString());
		}
	}

	private static class VisibilityStack extends Stack<Visibility> {

	}

	private static class Visibility {

		private final int value;
		private final IASTNode ast;

		private Visibility(int value, IASTNode ast) {
			this.value = value;
			this.ast = ast;
		}

		public IASTNode ast() {
			return ast;
		}

		public int value() {
			return value;
		}
	}
}