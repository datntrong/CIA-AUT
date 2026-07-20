package uet.fit.aut.testcase;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.cdt.core.dom.ast.IASTDeclarationStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTSimpleDeclaration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uet.fit.aut.exception.FunctionNodeNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.parser.dependency.Dependency;
import uet.fit.aut.parser.dependency.TypeDependency;
import uet.fit.aut.parser.obj.AvailableTypeNode;
import uet.fit.aut.parser.obj.ClassNode;
import uet.fit.aut.parser.obj.DefinitionFunctionNode;
import uet.fit.aut.parser.obj.ICommonFunctionNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.parser.obj.ISourcecodeFileNode;
import uet.fit.aut.parser.obj.InstanceVariableNode;
import uet.fit.aut.parser.obj.InternalVariableNode;
import uet.fit.aut.parser.obj.NumberOfCallNode;
import uet.fit.aut.parser.obj.ProjectNode;
import uet.fit.aut.parser.obj.ReturnVariableNode;
import uet.fit.aut.parser.obj.STLTypeNode;
import uet.fit.aut.parser.obj.VariableNode;
import uet.fit.aut.search.Search;
import uet.fit.aut.search.condition.AbstractFunctionNodeCondition;
import uet.fit.aut.search.condition.ClassNodeCondition;
import uet.fit.aut.search.condition.DefinitionFunctionNodeCondition;
import uet.fit.aut.search.condition.MacroFunctionNodeCondition;
import uet.fit.aut.search.condition.SourcecodeFileNodeCondition;
import uet.fit.aut.search.condition.StructurevsTypedefCondition;
import uet.fit.aut.search.condition.VariableNodeCondition;
import uet.fit.aut.testdata.InputCellHandler;
import uet.fit.aut.testdata.Iterator;
import uet.fit.aut.testdata.comparable.AssertMethod;
import uet.fit.aut.testdata.object.ClassDataNode;
import uet.fit.aut.testdata.object.ConstructorDataNode;
import uet.fit.aut.testdata.object.DataNode;
import uet.fit.aut.testdata.object.EnumDataNode;
import uet.fit.aut.testdata.object.FunctionPointerDataNode;
import uet.fit.aut.testdata.object.GlobalRootDataNode;
import uet.fit.aut.testdata.object.IDataNode;
import uet.fit.aut.testdata.object.MacroSubprogramDataNode;
import uet.fit.aut.testdata.object.MultipleDimensionCharacterDataNode;
import uet.fit.aut.testdata.object.MultipleDimensionDataNode;
import uet.fit.aut.testdata.object.MultipleDimensionNumberDataNode;
import uet.fit.aut.testdata.object.MultipleDimensionPointerDataNode;
import uet.fit.aut.testdata.object.MultipleDimensionStringDataNode;
import uet.fit.aut.testdata.object.NormalCharacterDataNode;
import uet.fit.aut.testdata.object.NormalDataNode;
import uet.fit.aut.testdata.object.NormalNumberDataNode;
import uet.fit.aut.testdata.object.NormalStringDataNode;
import uet.fit.aut.testdata.object.OneDimensionCharacterDataNode;
import uet.fit.aut.testdata.object.OneDimensionDataNode;
import uet.fit.aut.testdata.object.OneDimensionNumberDataNode;
import uet.fit.aut.testdata.object.OneDimensionPointerDataNode;
import uet.fit.aut.testdata.object.OneDimensionStringDataNode;
import uet.fit.aut.testdata.object.OtherUnresolvedDataNode;
import uet.fit.aut.testdata.object.PointerCharacterDataNode;
import uet.fit.aut.testdata.object.PointerDataNode;
import uet.fit.aut.testdata.object.PointerNumberDataNode;
import uet.fit.aut.testdata.object.PointerStringDataNode;
import uet.fit.aut.testdata.object.QTDataNode;
import uet.fit.aut.testdata.object.RootDataNode;
import uet.fit.aut.testdata.object.StructDataNode;
import uet.fit.aut.testdata.object.SubClassDataNode;
import uet.fit.aut.testdata.object.SubprogramNode;
import uet.fit.aut.testdata.object.TemplateSubprogramDataNode;
import uet.fit.aut.testdata.object.UnionDataNode;
import uet.fit.aut.testdata.object.UnitNode;
import uet.fit.aut.testdata.object.ValueDataNode;
import uet.fit.aut.testdata.object.VoidPointerDataNode;
import uet.fit.aut.testdata.object.stl.AllocatorDataNode;
import uet.fit.aut.testdata.object.stl.DefaultDeleteDataNode;
import uet.fit.aut.testdata.object.stl.ListBaseDataNode;
import uet.fit.aut.testdata.object.stl.PairDataNode;
import uet.fit.aut.testdata.object.stl.STLDataNode;
import uet.fit.aut.testdata.object.stl.SmartPointerDataNode;
import uet.fit.aut.testdata.object.stl.StdFunctionDataNode;
import uet.fit.aut.usercode.objects.AssertUserCode;
import uet.fit.aut.usercode.objects.UsedParameterUserCode;
import uet.fit.aut.util.IRegex;
import uet.fit.aut.util.NodeType;
import uet.fit.aut.util.PathUtils;
import uet.fit.aut.util.SourceConstant;
import uet.fit.aut.util.SpecialCharacter;
import uet.fit.aut.util.TemplateUtils;
import uet.fit.aut.util.Utils;
import uet.fit.aut.util.VariableTypeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestCaseDataImporter {

	private final static Logger logger = LoggerFactory.getLogger(TestCaseDataImporter.class);
	@NotNull
	private final ProjectNode projectRoot;
	@Nullable
	private IDataTestItem testCase;

	public TestCaseDataImporter(@NotNull ProjectNode root) {
		this.projectRoot = root;
	}

	public RootDataNode importRootDataNode(String jsonContent) {
		JsonObject json = new JsonParser().parse(jsonContent).getAsJsonObject();

		// change serialization for specific types
		JsonDeserializer<DataNode> deserializer = (json1, typeOfT, context) -> {
			JsonObject jsonObject = json1.getAsJsonObject();
			DataNode node = null;
			ValueDataNode dataNode = null;

			try {
				String type = jsonObject.get("type").getAsString();
				node = (DataNode) Class.forName(type).newInstance();

				String name = jsonObject.get("name").getAsString();
				node.setName(name);

				String dataType = "", realType = "", assertMethod;
				boolean external;

				if (node instanceof ValueDataNode) {
					dataNode = (ValueDataNode) node;

					JsonElement tempDataTypeJsonElement = jsonObject.get("dataType");
					if (tempDataTypeJsonElement != null) {
						dataType = tempDataTypeJsonElement.getAsString();
						dataNode.setRawType(dataType);
					}

					JsonElement tempRealTypeJsonElement = jsonObject.get("realType");
					if (tempDataTypeJsonElement != null) {
						realType = tempRealTypeJsonElement.getAsString();
						dataNode.setRealType(realType);
					}

					JsonElement tempExternalJsonElement = jsonObject.get("external");
					if (tempExternalJsonElement != null) {
						external = tempExternalJsonElement.getAsBoolean();
						dataNode.setExternel(external);
					}

					JsonElement tempAssertMethodJsonElement = jsonObject.get("assertMethod");
					if (tempAssertMethodJsonElement != null) {
						assertMethod = tempAssertMethodJsonElement.getAsString();
						dataNode.setAssertMethod(assertMethod);

						if (AssertMethod.USER_CODE.equals(assertMethod)) {
							if (jsonObject.get("assertUserCode") != null) {
								JsonObject assertUserCodeJsonObject = jsonObject.get("assertUserCode").getAsJsonObject();
								AssertUserCode assertUserCode = getUserCodeFromJsonObject(assertUserCodeJsonObject, AssertUserCode.class);
								dataNode.setAssertUserCode(assertUserCode);
							}
						}
					}

					if (jsonObject.get("userCode") != null) {
						JsonObject userCodeJsonObject = (JsonObject) jsonObject.get("userCode");
						UsedParameterUserCode userCode = getUserCodeFromJsonObject(userCodeJsonObject);
						dataNode.setUserCode(userCode);
						dataNode.setUseUserCode(true);
						if (testCase != null)
							testCase.putOrUpdateDataNodeIncludes(dataNode);
					}
				}

				if (node instanceof MacroSubprogramDataNode) {
					dataNode.setRawType(dataType);
					dataNode.setRealType(realType);
				}

				if (node instanceof RootDataNode) {
					String level = jsonObject.get("level").getAsString();
					NodeType nodeType = stringToNodeType(level);

					if (nodeType == NodeType.GLOBAL
							&& !(node instanceof GlobalRootDataNode)) {
						node = new GlobalRootDataNode();
						node.setName(name);
					}

					RootDataNode root = (RootDataNode) node;

					if (nodeType != null)
						root.setLevel(nodeType);

					if (jsonObject.get("functionNode") != null) {
						String path = jsonObject.get("functionNode").getAsString();
						ICommonFunctionNode functionNode = searchFunctionNodeByPath(path);
						root.setFunctionNode(functionNode);
					}

				} else if (node instanceof UnitNode) {
					UnitNode unit = (UnitNode) node;

					String sourcePath = jsonObject.get("sourceNode").getAsString();

					List<INode> sourceCodeFileNodes = Search.searchNodes(projectRoot, new SourcecodeFileNodeCondition());
					for (INode scfnode : sourceCodeFileNodes) {
						ISourcecodeFileNode cast = (ISourcecodeFileNode) scfnode;
						if (PathUtils.equals(cast.getAbsolutePath(), sourcePath))
							unit.setSourceNode(cast);
					}
//                        List<INode> possibles = Search.searchNodes(
//                                Environment.getInstance().getProjectNode(), new SourcecodeFileNodeCondition(), Utils.normalizePath(sourcePath));
//                        if (!possibles.isEmpty())
//                            unit.setSourceNode(possibles.get(0));

//                        boolean stubChildren = jsonObject.get("stubChildren").getAsBoolean();
//                        unit.setStubChildren(stubChildren);

//                } else if (dataNode.isUseUserCode()) { // boolean useUserCode is set upon
//                    // do nothing

				} else if (dataNode instanceof NumberOfCallNode) {
					String value = jsonObject.get("value").getAsString();
					if (!value.equals("null"))
						((NumberOfCallNode) dataNode).setValue(value);

				} else if (dataNode instanceof NormalCharacterDataNode
						|| dataNode instanceof NormalStringDataNode
						|| dataNode instanceof NormalNumberDataNode) {
					loadCorrespondingDependency(jsonObject, dataNode);

					if (!dataNode.isUseUserCode()) {
						String value = jsonObject.get("value").getAsString();
						if (!value.equals("null"))
							((NormalDataNode) dataNode).setValue(value);
					}

				} else if (dataNode instanceof PointerDataNode
						&& jsonObject.get("correspondingVar") != null) {

					if (loadCorrespondingDependency(jsonObject, dataNode)
							&& jsonObject.get("size") != null && jsonObject.get("level") != null) {
						int level = jsonObject.get("level").getAsInt();
						((PointerDataNode) dataNode).setLevel(level);

						int size = jsonObject.get("size").getAsInt();
//                        if (size >= 0) {
						((PointerDataNode) dataNode).setAllocatedSize(size);
						((PointerDataNode) dataNode).setSizeIsSet(true);
//                        }
					}

				} else if (dataNode instanceof OneDimensionDataNode
						&& jsonObject.get("correspondingVar") != null) {

					if (loadCorrespondingDependency(jsonObject, dataNode) && jsonObject.get("size") != null) {
						int size = jsonObject.get("size").getAsInt();
//                        if (size >= 0) {
						((OneDimensionDataNode) dataNode).setSize(size);
						((OneDimensionDataNode) dataNode).setSizeIsSet(true);
//                        }
					}

					boolean fixedSize = jsonObject.get("fixedSize").getAsBoolean();
					((OneDimensionDataNode) dataNode).setFixedSize(fixedSize);

				} else if (dataNode instanceof MultipleDimensionDataNode
						&& jsonObject.get("correspondingVar") != null) {

					if (loadCorrespondingDependency(jsonObject, dataNode)
							&& jsonObject.get("size") != null && jsonObject.get("dimensions") != null) {
						int dimensions = jsonObject.get("dimensions").getAsInt();
						String[] sizesInString = jsonObject.get("size").getAsString().split(", ");

						if (dimensions == sizesInString.length) {
							int[] sizes = new int[dimensions];

							for (int i = 0; i < dimensions; i++) {
								sizes[i] = Integer.parseInt(sizesInString[i]);
							}

							((MultipleDimensionDataNode) dataNode).setSizes(sizes);
							((MultipleDimensionDataNode) dataNode).setSizeIsSet(sizes[0] > 0);
						}

						((MultipleDimensionDataNode) dataNode).setDimensions(dimensions);
					}


					boolean fixedSize = jsonObject.get("fixedSize").getAsBoolean();
					((MultipleDimensionDataNode) dataNode).setFixedSize(fixedSize);

				} else if (dataNode instanceof ConstructorDataNode
						&& jsonObject.get("functionNode") != null) { // need to set function node for it
					String path = jsonObject.get("functionNode").getAsString();

					try {
						INode functionNode = searchFunctionNodeByPath(path);
						((ConstructorDataNode) dataNode).setFunctionNode(functionNode);
					} catch (Exception e) {

						IASTNode astNode = Utils.convertToIAST(path);

						if (astNode instanceof IASTDeclarationStatement) {
							astNode = ((IASTDeclarationStatement) astNode).getDeclaration();
						}

						if (astNode instanceof CPPASTSimpleDeclaration) {
							DefinitionFunctionNode functionNode = new DefinitionFunctionNode();

							String ptrPath = jsonObject.get("pointer_path").getAsString();

							functionNode.setAbsolutePath(ptrPath);
							functionNode.setAST((CPPASTSimpleDeclaration) astNode);
							functionNode.setName(functionNode.getNewType());
							((ConstructorDataNode) dataNode).setFunctionNode(functionNode);
						}
					}


				} else if (dataNode instanceof SubprogramNode) { // need to set function node for it
					String path = jsonObject.get("functionNode").getAsString();

					INode functionNode = searchFunctionNodeByPath(path);
					((SubprogramNode) dataNode).setFunctionNode(functionNode);

					if (dataNode instanceof MacroSubprogramDataNode
							|| dataNode instanceof TemplateSubprogramDataNode) {
						dataNode.setRawType(dataType);
						dataNode.setRealType(realType);
					}

					if (dataNode instanceof TemplateSubprogramDataNode)
						// load the template parameters of template functions
						if (jsonObject.get(TemplateSubprogramDataNode.NAME_TEMPLATE_TYPE) != null) {
							JsonArray template_types = jsonObject.getAsJsonArray(TemplateSubprogramDataNode.NAME_TEMPLATE_TYPE);
							Map<String, String> realTypeMapping = new HashMap<>();
							for (JsonElement element : template_types) {
//                                    String str = element.getAsString().substring(1, element.getAsString().length()-1);
								realTypeMapping.put(element.getAsString().split("->")[0],
										element.getAsString().split("->")[1]);
							}
							((TemplateSubprogramDataNode) dataNode).setRealTypeMapping(realTypeMapping);
						}

					if (dataNode instanceof MacroSubprogramDataNode)
						// load the template parameters of macro functions
						if (jsonObject.get(MacroSubprogramDataNode.NAME_MACRO_TYPE) != null) {
							JsonArray macro_types = jsonObject.getAsJsonArray(MacroSubprogramDataNode.NAME_MACRO_TYPE);
							Map<String, String> realTypeMapping = new HashMap<>();
							for (JsonElement element : macro_types) {
//                                    String str = element.getAsString().substring(1, element.getAsString().length()-1);
								realTypeMapping.put(element.getAsString().split("->")[0],
										element.getAsString().split("->")[1]);
							}
							((MacroSubprogramDataNode) dataNode).setRealTypeMapping(realTypeMapping);
						}

				} else if (dataNode instanceof SubClassDataNode
						&& jsonObject.get("correspondingType") != null
						&& jsonObject.get("correspondingVar") != null) {

					if (loadCorrespondingDependency(jsonObject, dataNode)
							&& jsonObject.get("selectedConstructor") != null) {
						VariableNode prevVar = dataNode.getCorrespondingVar();

						if (!dataType.equals(VariableTypeUtils.getFullRawType(prevVar))) {
							String classPath = jsonObject.get("correspondingType").getAsString();

							List<INode> nodes = Search.searchNodes(projectRoot, new ClassNodeCondition());

							//TODO
//                            INode dataRoot = Environment.getInstance().getUserCodeRoot();
//                            nodes.addAll(Search.searchNodes(dataRoot, new ClassNodeCondition()));

							for (INode n : nodes) {
								ClassNode cast = (ClassNode) n;
								if (PathUtils.equals(cast.getAbsolutePath(), classPath)) {
									dataNode.setCorrespondingVar(refactorVariableType(dataType, prevVar, n));
									break;
								}
							}
							if (dataNode.getCorrespondingVar() == null) {
								logger.debug("Failed to search corresponding var of the data node: " + dataNode.getName());
							}
						}

						String constructorName = jsonObject.get("selectedConstructor").getAsString();
						if (constructorName != null) {
							try {
								((SubClassDataNode) dataNode).chooseConstructor(constructorName);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				} else if (dataNode instanceof ClassDataNode
						&& jsonObject.get("correspondingVar") != null
						&& jsonObject.get("correspondingType") != null) {

					if (loadCorrespondingDependency(jsonObject, dataNode)) {
						VariableNode prevVar = dataNode.getCorrespondingVar();

						if (!dataType.equals(VariableTypeUtils.getFullRawType(prevVar))) {
							String classPath = jsonObject.get("correspondingType").getAsString();

							List<INode> nodes = Search.searchNodes(projectRoot, new ClassNodeCondition());
							//TODO
//                            INode dataRoot = Environment.getInstance().getUserCodeRoot();
//                            nodes.addAll(Search.searchNodes(dataRoot, new ClassNodeCondition()));

							for (INode n : nodes) {
								ClassNode cast = (ClassNode) n;
								if (PathUtils.equals(cast.getAbsolutePath(), classPath)) {
									dataNode.setCorrespondingVar(refactorVariableType(dataType, prevVar, n));
									break;
								}
							}
							if (dataNode.getCorrespondingVar() == null) {
								logger.debug("Failed to search corresponding var of the data node: " + dataNode.getName());
							}
						}
					}

				} else if (dataNode instanceof EnumDataNode
						&& jsonObject.get("correspondingVar") != null
						&& jsonObject.get("correspondingType") != null) {

					if (loadCorrespondingDependency(jsonObject, dataNode)
							&& jsonObject.get("value") != null && jsonObject.get("valueIsSet") != null) {
						String value = jsonObject.get("value").getAsString();
						String valueIsSet = jsonObject.get("valueIsSet").getAsString();
						if (valueIsSet.equals("true")) {
							((EnumDataNode) dataNode).setValue(value);
							((EnumDataNode) dataNode).setValueIsSet(true);
						}
					}
				} else if (dataNode instanceof UnionDataNode
						&& jsonObject.get("correspondingVar") != null
						&& jsonObject.get("correspondingType") != null) {

					if (jsonObject.get("selectedField") != null) {
						String field = jsonObject.get("selectedField").getAsString();
						((UnionDataNode) dataNode).setField(field);
					}

					if (!loadCorrespondingDependency(jsonObject, dataNode))
						logger.error("Can not load corresponding var for UnionDataNode: " + dataNode.getName());

				} else if (dataNode instanceof StructDataNode
						&& jsonObject.get("correspondingVar") != null
						&& jsonObject.get("correspondingType") != null) {

					if (!loadCorrespondingDependency(jsonObject, dataNode))
						logger.error("Can not load corresponding var for UnionDataNode: " + dataNode.getName());

				} else if (dataNode instanceof QTDataNode
						&& jsonObject.get("correspondingVar") != null) {

					JsonArray jsonConstructors = jsonObject.get("constructors").getAsJsonArray();
					for (JsonElement jsonCons : jsonConstructors) {
						QTDataNode.QTConstructorNode cons = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()
								.fromJson(jsonCons, QTDataNode.QTConstructorNode.class);
						((QTDataNode) dataNode).getConstructorNodes().add(cons);
					}

					JsonElement selectedCons = jsonObject.get("selectedConstructor");
					if (selectedCons != null) {
						QTDataNode.QTConstructorNode selectedConstructor = ((QTDataNode) dataNode).getConstructorNodes()
								.stream().filter(cons -> cons.getName().equals(selectedCons.getAsString()))
								.findFirst()
								.orElse(null);
						((QTDataNode) dataNode).setSelectedConstructor(selectedConstructor);
					}

					if (!loadCorrespondingDependency(jsonObject, dataNode))
						logger.error("Can not load corresponding var for UnionDataNode: " + dataNode.getName());

				} else if (dataNode instanceof ListBaseDataNode) {
					if (loadCorrespondingDependency(jsonObject, dataNode)) {
						ListBaseDataNode listBaseDataNode = (ListBaseDataNode) dataNode;

						String templateArg = jsonObject.get("templateArg").getAsString();

						List<String> arguments = new ArrayList<>();
						arguments.add(templateArg);
						listBaseDataNode.setArguments(arguments);

						if (jsonObject.get("size") != null) {
							int size = jsonObject.get("size").getAsInt();
							listBaseDataNode.setSize(size);
							listBaseDataNode.setSizeIsSet(true);
						}
					}

				} else if (dataNode instanceof StdFunctionDataNode) {
					if (loadCorrespondingDependency(jsonObject, dataNode)) {
						StdFunctionDataNode lambdaDataNode = (StdFunctionDataNode) dataNode;

						String templateArg = jsonObject.get("templateArg").getAsString();
						List<String> arguments = new ArrayList<>();
						arguments.add(templateArg);
						lambdaDataNode.setArguments(arguments);

//                        if (jsonObject.get("userCode") != null) {
//                            JsonObject userCodeJsonObject = (JsonObject) jsonObject.get("userCode");
//                            UsedParameterUserCode userCode = getUserCodeFromJsonObject(userCodeJsonObject);
//                            lambdaDataNode.setUserCode(userCode);
//                            testCase.putOrUpdateDataNodeIncludes(dataNode);
//                        }
					}

				} else if (dataNode instanceof PairDataNode) {
					if (loadCorrespondingDependency(jsonObject, dataNode)) {
						JsonObject templateArg = jsonObject.get("templateArg").getAsJsonObject();
						String first = templateArg.get("first").getAsString();
						String second = templateArg.get("second").getAsString();

						List<String> arguments = Arrays.asList(first, second);
						((STLDataNode) dataNode).setArguments(arguments);
					}

				} else if (dataNode instanceof SmartPointerDataNode
						|| dataNode instanceof AllocatorDataNode
						|| dataNode instanceof DefaultDeleteDataNode) {
					if (loadCorrespondingDependency(jsonObject, dataNode)) {
						List<String> arguments = new ArrayList<>();

						if (jsonObject.get("templateArg") != null) {
							String templateArg = jsonObject.get("templateArg").getAsString();
							arguments.add(templateArg);
						}

						((STLDataNode) dataNode).setArguments(arguments);
					}

				} else if (dataNode instanceof FunctionPointerDataNode) {
					if (loadCorrespondingDependency(jsonObject, dataNode)) {
						if (jsonObject.get("reference") != null) {
							String reference = jsonObject.get("reference").getAsString();
							ICommonFunctionNode selectedFunction = searchFunctionNodeByPath(reference);

							InputCellHandler handler = new InputCellHandler();
							handler.setTestCase(testCase);
							handler.commitSelectedReference((FunctionPointerDataNode) dataNode, selectedFunction);
						}
					}
				} else if (dataNode instanceof VoidPointerDataNode) {
					if (loadCorrespondingDependency(jsonObject, dataNode)) {
						JsonElement jsonElement = jsonObject.get("referType");
						if (jsonElement != null) {
							String referType = jsonElement.getAsString();
							((VoidPointerDataNode) dataNode).setReferenceType(referType);
						}

						jsonElement = jsonObject.get("inputMethod");
						if (jsonElement != null) {
							String im = jsonElement.getAsString();
							if (im.equals(VoidPointerDataNode.InputMethod.AVAILABLE_TYPES.toString())) {
								((VoidPointerDataNode) dataNode).setInputMethod(VoidPointerDataNode
										.InputMethod.AVAILABLE_TYPES);

								JsonElement tempIncludeJsonElement = jsonObject.get("referTypeInclude");
								if (tempIncludeJsonElement != null && testCase != null) {
									String include = tempIncludeJsonElement.getAsString();
									testCase.putOrUpdateDataNodeIncludes(dataNode, include);
								}

							} else if (im.equals(VoidPointerDataNode.InputMethod.USER_CODE.toString())) {
								((VoidPointerDataNode) dataNode).setInputMethod(VoidPointerDataNode
										.InputMethod.USER_CODE);
//                                JsonObject userCodeJsonObject = (JsonObject) jsonObject.get("userCode");
//                                UsedParameterUserCode userCode = getUserCodeFromJsonObject(userCodeJsonObject);
//                                ((VoidPointerDataNode) dataNode).setUserCode(userCode);
//                                testCase.putOrUpdateDataNodeIncludes(dataNode);
							} else {
								logger.error("Invalid input method.");
							}
						}
					}
				} else if (dataNode instanceof OtherUnresolvedDataNode) {
					if (loadCorrespondingDependency(jsonObject, dataNode)) {
//                        JsonObject userCodeJsonObject = (JsonObject) jsonObject.get("userCode");
//                        if (userCodeJsonObject != null) {
//                            UsedParameterUserCode userCode = getUserCodeFromJsonObject(userCodeJsonObject);
//                            ((OtherUnresolvedDataNode) dataNode).setUserCode(userCode);
//                            testCase.putOrUpdateDataNodeIncludes(dataNode);
//                        }
					}
				}

				// load children
				if (jsonObject.get("children") != null) {
					for (JsonElement child : jsonObject.get("children").getAsJsonArray()) {
						DataNode childNode = context.deserialize(child, DataNode.class);
						//TODO: build project tree
						if (childNode instanceof QTDataNode.QTConstructorNode && node instanceof QTDataNode) {
							QTDataNode qtDataNode = (QTDataNode) node;
							QTDataNode.QTConstructorNode qtConstructorNode = (QTDataNode.QTConstructorNode) childNode;
							qtDataNode.getChildren().remove(qtConstructorNode);
							QTDataNode.QTConstructorNode realConsNode = qtDataNode.getSelectedConstructor();
							for (IDataNode param : qtConstructorNode.getChildren()) {
								generateTreeDependency(realConsNode, (DataNode) param);
							}
						} else if (childNode != null)
							generateTreeDependency(node, childNode);
					}
				}

				// if dataNode is sut, load expected outputs of parameters
				if (dataNode instanceof SubprogramNode) {
					if (jsonObject.get("paramExpectedOuputs") != null) {
						for (JsonElement eo : jsonObject.get("paramExpectedOuputs").getAsJsonArray()) {
							DataNode eoDataNode = context.deserialize(eo, DataNode.class);
							if (eoDataNode != null) {
								eoDataNode.setParent(node);
								((SubprogramNode) dataNode).putParamExpectedOutputs((ValueDataNode) eoDataNode);
							}
						}
					}
				} else if (node instanceof GlobalRootDataNode && ((RootDataNode) node).getLevel().equals(NodeType.GLOBAL)) {
					if (jsonObject.get("paramExpectedOuputs") != null) {
						((GlobalRootDataNode) node).setGlobalInputExpOutputMap(new HashMap<>());
						for (JsonElement eo : jsonObject.get("paramExpectedOuputs").getAsJsonArray()) {
							DataNode eoDataNode = context.deserialize(eo, DataNode.class);
							if (eoDataNode != null) {
								eoDataNode.setParent(node);
								if (!((GlobalRootDataNode) node).putGlobalExpectedOutput((ValueDataNode) eoDataNode)) {
									logger.debug("Failed when import and put expected output to GlobalInputExpectedOutputMap");
								}
							}
						}
					}
				}

				// load iterator
				if (jsonObject.get("iterators") != null && dataNode != null) {
					JsonArray iteratorsJsonArray = jsonObject.get("iterators").getAsJsonArray();
					List<Iterator> iterators = loadIterators(context, dataNode, iteratorsJsonArray);
					dataNode.setIterators(iterators);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			return node;
		};

		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(DataNode.class, deserializer);
		Gson customGson = gsonBuilder.create();
		return (RootDataNode) customGson.fromJson(json, DataNode.class);
	}

	private UsedParameterUserCode getUserCodeFromJsonObject(JsonObject jsonObject) {
		return getUserCodeFromJsonObject(jsonObject, UsedParameterUserCode.class);
	}

	public <T extends UsedParameterUserCode> T getUserCodeFromJsonObject(JsonObject jsonObject, Class<? extends UsedParameterUserCode> clazz) {
		UsedParameterUserCode userCode = null;

		try {
			userCode = clazz.newInstance();

			if (jsonObject.get("type") != null && jsonObject.get("type").getAsString().equals(UsedParameterUserCode.TYPE_CODE)) {
				// type, content, include paths
				userCode.setType(UsedParameterUserCode.TYPE_CODE);
				userCode.setContent(jsonObject.get("content").getAsString());
				JsonArray includePaths = jsonObject.get("includePaths").getAsJsonArray();
				for (JsonElement element : includePaths) {
					userCode.getIncludePaths().add(element.getAsString());
				}
				//        } else if (jsonObject.get("type").getAsString().equals(UsedDefineArgumentUserCode.TYPE_REFERENCE)) {
			} else { // UsedDefineArgumentUserCode.TYPE_REFERENCE
				// get the user code has correspond id
				int id = jsonObject.get("id").getAsInt();
				userCode.setType(UsedParameterUserCode.TYPE_REFERENCE);
				userCode.setId(id);
			}

		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}

		return (T) userCode;
	}

	private List<Iterator> loadIterators(JsonDeserializationContext context, ValueDataNode node, JsonArray iteratorsJsonArray) {
		List<Iterator> iterators = new ArrayList<>();

		for (JsonElement iteratorJsonElement : iteratorsJsonArray) {
			JsonObject iteratorJsonObj = iteratorJsonElement.getAsJsonObject();

			Iterator iterator = new Iterator();

			int start = iteratorJsonObj.get("start").getAsInt();
			iterator.setStartIdx(start);

			int repeat = iteratorJsonObj.get("repeat").getAsInt();
			iterator.setRepeat(repeat);

			JsonElement dataNodeJsonObj = iteratorJsonObj.get("dataNode");

			if (dataNodeJsonObj == null) {
				iterator.setDataNode(node);
			} else {
				ValueDataNode childNode = context.deserialize(dataNodeJsonObj, DataNode.class);
				iterator.setDataNode(childNode);
				childNode.setIterators(iterators);
			}

			iterators.add(iterator);
		}

		return iterators;
	}

	private VariableNode refactorVariableType(String dataType, VariableNode prevVar, INode classNode) {
		return VariableTypeUtils.cloneAndReplaceType(dataType, prevVar, classNode);
	}

	private boolean loadCorrespondingDependency(JsonObject jsonObject, ValueDataNode dataNode) {
		if (jsonObject.get("correspondingVar") != null) {
			String absolutePath = jsonObject.get("correspondingVar").getAsString();

			boolean success = loadCorrespondingVar(dataNode, absolutePath);
			if (jsonObject.get("correspondingType") != null) {
				// can not find the var on physical tree
				if (!success) {
					String correspondingType = jsonObject.get("correspondingType").getAsString();
					success = loadVariableFromCorrespondingType(dataNode,
							correspondingType, absolutePath);
				}
			} else {
				// can not find the var on physical tree
				if (!success) {
					VariableNode v = createVariableNode(dataNode, absolutePath);
					dataNode.setCorrespondingVar(v);

					success = true;
				}
			}

			return success;
		} else {
			return false;
		}
	}

	private void generateTreeDependency(DataNode parent, DataNode child) {
		// set subClass
		if (child instanceof SubClassDataNode && parent instanceof ClassDataNode)
			((ClassDataNode) parent).setSubClass((SubClassDataNode) child);
		else {
			parent.getChildren().add(child);
			child.setParent(parent);

			if (child instanceof ValueDataNode) {
				List<Iterator> iterators = ((ValueDataNode) child).getIterators();

				if (iterators.size() > 1) {
					for (int i = 1; i < iterators.size(); i++) {
						iterators.get(i).getDataNode().setParent(parent);
					}
				}
			}
		}
	}

	private boolean loadCorrespondingVar(ValueDataNode dataNode, String absolutePath) {
		String relativePath = absolutePath.substring(absolutePath.indexOf(File.separator));
		List<INode> nodes = Search.searchNodes(projectRoot, new VariableNodeCondition(), relativePath);

		if (nodes.stream().anyMatch(n -> n instanceof InternalVariableNode)) {
			nodes.removeIf(n -> !(n instanceof InternalVariableNode));
		}

		if (nodes.size() == 1) {
			VariableNode variableNode = (VariableNode) nodes.get(0);
			// set corresponding variable for subclass data node
			dataNode.setCorrespondingVar(variableNode);
			return true;
		}
		//TODO
//        nodes = Search.searchNodes(Environment.getInstance().getSystemLibraryRoot(), new VariableNodeCondition(), relativePath);
//
//        if (nodes.size() == 1) {
//            VariableNode variableNode = (VariableNode) nodes.get(0);
//            // set corresponding variable for subclass data node
//            dataNode.setCorrespondingVar(variableNode);
//            return true;
//        }

//        nodes = Search.searchNodes(Environment.getInstance().getDataUserCodeRoot(), new VariableNodeCondition(), relativePath);
//        if (nodes.size() == 1) {
//            VariableNode variableNode = (VariableNode) nodes.get(0);
//            // set corresponding variable for subclass data node
//            dataNode.setCorrespondingVar(variableNode);
//            return true;
//        }

		return false;
	}

	private boolean loadVariableFromCorrespondingType(ValueDataNode dataNode,
			String typeAbsolutePath, String varAbsolutePath) {
		// Find corresponding type node in project tree.
		INode type = getType(typeAbsolutePath);

		if (type == null) {
			if (dataNode instanceof NormalDataNode
					|| dataNode instanceof OneDimensionCharacterDataNode || dataNode instanceof OneDimensionNumberDataNode
					|| dataNode instanceof OneDimensionStringDataNode || dataNode instanceof OneDimensionPointerDataNode
					|| dataNode instanceof MultipleDimensionCharacterDataNode || dataNode instanceof MultipleDimensionNumberDataNode
					|| dataNode instanceof MultipleDimensionStringDataNode || dataNode instanceof MultipleDimensionPointerDataNode
					|| dataNode instanceof PointerNumberDataNode || dataNode instanceof PointerCharacterDataNode
					|| dataNode instanceof PointerStringDataNode) {
				type = new AvailableTypeNode();
				((AvailableTypeNode) type).setType(dataNode.getRawType());
			} else if (dataNode instanceof STLDataNode) {
				type = new STLTypeNode();
				((STLTypeNode) type).setType(dataNode.getRawType());
			} else
				return false;
		}

		// STEP1: Create variable node & add property
		VariableNode v = createVariableNode(dataNode, varAbsolutePath);

		// STEP2: Generate type dependency
		v.setTypeDependencyState(true);
		v.setCorrespondingNode(type);
		Dependency dependency = new TypeDependency(v, type);
		type.getDependencies().add(dependency);
		v.getDependencies().add(dependency);

		dataNode.setCorrespondingVar(v);

		return true;
	}

	private INode getType(String absolutePath) {
		// Search Level 2
		List<INode> structureNodes = Search.searchNodes(projectRoot, new StructurevsTypedefCondition());
		for (INode structureNode : structureNodes) {
			if (PathUtils.equals(structureNode.getAbsolutePath(), absolutePath)) {
				return structureNode;
			}
		}
		return null;
	}

	private VariableNode createVariableNode(ValueDataNode dataNode, String varAbsolutePath) {
		VariableNode v = new VariableNode();
		String name = dataNode.getName();
		String rType = dataNode.getRawType();

		if (name.equals("RETURN"))
			v = new ReturnVariableNode();
		else if (name.startsWith(SourceConstant.INSTANCE_VARIABLE))
			v = new InstanceVariableNode();

		v.setName(name);
		v.setRawType(rType);
		v.setReducedRawType(rType);

		String cType;
		if (TemplateUtils.isTemplate(rType))
			cType = rType.substring(0, rType.lastIndexOf(TemplateUtils.CLOSE_TEMPLATE_ARG) + 1);
		else
			cType = rType.replaceAll(IRegex.POINTER, SpecialCharacter.EMPTY)
					.replaceAll(IRegex.ARRAY_INDEX, SpecialCharacter.EMPTY);
		v.setCoreType(cType);

		v.setAbsolutePath(varAbsolutePath);

		return v;
	}

	private ICommonFunctionNode searchFunctionNodeByPath(String path) throws FunctionNodeNotFoundException {
		List<ICommonFunctionNode> completedFunctions = Search.searchNodes(projectRoot, new AbstractFunctionNodeCondition(), path);
		if (!completedFunctions.isEmpty())
			return completedFunctions.get(0);

		List<ICommonFunctionNode> declaredFunctions = Search.searchNodes(projectRoot, new DefinitionFunctionNodeCondition(), path);
		if (!declaredFunctions.isEmpty())
			return declaredFunctions.get(0);

		List<ICommonFunctionNode> macroFunctions = Search.searchNodes(projectRoot, new MacroFunctionNodeCondition(), path);
		if (!macroFunctions.isEmpty())
			return macroFunctions.get(0);

		throw new FunctionNodeNotFoundException(path);
	}

	private NodeType stringToNodeType(String type) {
		NodeType nodeType = null;

		switch (type) {
			case "ROOT":
				nodeType = NodeType.ROOT;
				break;
			case "UUT":
				nodeType = NodeType.UUT;
				break;
			case "GLOBAL":
				nodeType = NodeType.GLOBAL;
				break;
			case "STUB":
				nodeType = NodeType.STUB;
				break;
			case "DONT_STUB":
				nodeType = NodeType.DONT_STUB;
				break;
			case "SBF":
				nodeType = NodeType.SBF;
				break;
			case "STATIC":
				nodeType = NodeType.STATIC;
				break;
		}

		return nodeType;
	}

	public @Nullable IDataTestItem getTestCase() {
		return testCase;
	}

	public void setTestCase(@Nullable IDataTestItem testCase) {
		this.testCase = testCase;
	}
}
