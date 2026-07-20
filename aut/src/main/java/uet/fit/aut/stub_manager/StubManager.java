package uet.fit.aut.stub_manager;

import uet.fit.aut.config.FolderConfig;
import uet.fit.aut.execution.DriverConstant;
import uet.fit.aut.logger.AUTLogger;
import uet.fit.aut.parser.obj.*;
import uet.fit.aut.search.Search2;
import uet.fit.aut.testcase.TestCase;
import uet.fit.aut.testdata.Iterator;
import uet.fit.aut.testdata.object.IDataNode;
import uet.fit.aut.testdata.object.IterationSubprogramNode;
import uet.fit.aut.testdata.object.SubprogramNode;
import uet.fit.aut.testdata.object.ValueDataNode;
import uet.fit.aut.util.*;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class StubManager {
	private static final AUTLogger logger = AUTLogger.get(StubManager.class);

	public static final String AKA_TEST_CASE_NAME = DriverConstant.TEST_NAME;

	private static boolean checkStaticNormalFunc = false;
	/**
	 * Function path -> Stub file path
	 */
	private static Map<String, String> fileList;

	// private static String generateIteratorBody(ValueDataNode dataNode) throws Exception {
	//     StringBuilder body = new StringBuilder();

	//     if (dataNode.isStubArgument()) {
	//         List<Iterator> iterators = dataNode.getIterators();

	//         for (Iterator iterator : iterators) {
	//             int start = iterator.getStartIdx();
	//             int repeat = iterator.getRepeat();
	//             int end = start + repeat;

	//             String source = iterator.getDataNode().getInputForGoogleTest(true);
	//             source += iterator.getDataNode().getInputForGoogleTest(false);

	//             if (!dataNode.getName().equals(RETURN_NAME)) {
	//                 source += SpecialCharacter.LINE_BREAK;
	//                 source += "#ifdef ASSERT_ENABLE\n";
	//                 source += iterator.getDataNode().getAssertion();
	//                 source += "\n#endif\n";
	//             } else {
	//                 source += String.format("return %s;", iterator.getDataNode().getVituralName());
	//             }

	//             body.append(String.format(ITERATOR_TEMPLATE, start, repeat, end, source));
	//         }
	//     }

	//     return body.toString();
	// }

	private static String genIterSubprogramNodeBody(IterationSubprogramNode node) throws Exception {
		StringBuilder body = new StringBuilder();

		List<IDataNode> children = node.getChildren();

		String source = "";

		for (IDataNode child : children) {
			if (child instanceof ValueDataNode) {
				ValueDataNode dataNode = (ValueDataNode) child;
				source += dataNode.getInputForGoogleTest(true);
				source += dataNode.getInputForGoogleTest(false);
				if (!dataNode.getName().equals(RETURN_NAME)) {
					source += SpecialCharacter.LINE_BREAK;
					source += "#ifdef ASSERT_ENABLE\n";
					source += dataNode.getAssertion();
					source += "\n#endif\n";
				} else {
					source += String.format("return %s;", dataNode.getVirtualName());
				}
			}
		}
		int index = node.getIndex();
		body.append(String.format(NEW_ITERATOR_TEMPLATE, index, source));

		return body.toString();
	}

	// private static final String ITERATOR_TEMPLATE =
	//         "if (AKA_INTRA_FCOUNT >= %d && (%d <= 0 || AKA_INTRA_FCOUNT < %d)) {\n" +
	//             "%s\n" +
	//         "}\n";

	private static final String NEW_ITERATOR_TEMPLATE =
			"if (AKA_INTRA_FCOUNT == %d) {\n" +
					"%s\n" +
					"}\n";

	private static final String RETURN_NAME = "RETURN";

	public static String generateStubMainCode(TestCase testCase) {
		String stubMainCode = SpecialCharacter.EMPTY;
		List<SubprogramNode> stubSubprograms = Search2.searchStubableSubprograms(testCase.getRootDataNode());
//        stubSubprograms.remove(stubSubprograms.size() - 1);
		String testCaseName = testCase.getName();
		try {
			for (SubprogramNode child : stubSubprograms) {
				if (child != null && !(child instanceof IterationSubprogramNode)) {
					SubprogramNode subprogram = (SubprogramNode) child;

					FunctionNode function = (FunctionNode) subprogram.getFunctionNode();
					String stubPath = getStubCodeFilePath(function.getAbsolutePath());

					String testCaseStubMainCode = SpecialCharacter.EMPTY;

					if (subprogram.isStub() && isVoid(subprogram) == false) {

						if (subprogram.getFunctionNode().getParent() instanceof ClassNode || subprogram.getFunctionNode().getParent() instanceof StructNode) {
							if (((FunctionNode) subprogram.getFunctionNode()).isVirtual()) {
								String template =
										"	typedef __int__ (*fptr)(ClassA*,__int__);\n" +
												"    fptr ClassA_foofunc = (fptr)(&ClassA::foofunc);   //obtaining an address\n" +
												"    Stub __stub__;\n" +
												"    __stub__.set(ClassA_foofunc, foo_stub);\n";
								String ClassName = function.getParent().getName();
								String functionName = function.getSimpleName();
								String functionStubName = Utils.getFullFunctionCallNameForStub(function);
								String returnTypeName = ((FunctionNode) subprogram.getFunctionNode()).getReturnType().trim();
								String nameStub = "stub_of_" + function.getSimpleName();
								testCaseStubMainCode = template.replace("__int__", returnTypeName)
										.replace("ClassA", ClassName)
										.replace("foofunc", functionName)
										.replace("foo_stub", functionStubName)
										.replace("__stub__", nameStub);
							} else {
								testCaseStubMainCode = "";
								testCaseStubMainCode += "Stub " + "stubOfforclass" + function.getParent().getName() + "_" + function.getSimpleName() + ";\n";
								testCaseStubMainCode += "stubOfforclass" + function.getParent().getName() + "_" + function.getSimpleName() + ".set(ADDR(" + function.getParent().getName() + "," +
										function.getSimpleName() + "), " + Utils.getFullFunctionCallNameForStub(function) + ");\n";
							}

						} else if (((FunctionNode) subprogram.getFunctionNode()).isStatic()) {
							checkStaticNormalFunc = true;
							String any = "any_" + function.getSimpleName();
							String result = "result_" + function.getSimpleName();
							String stub = "stubStatic_" + function.getSimpleName();
							String it = "it_" + function.getSimpleName();

							String testDriverTemplateMain = "" +
									"        AddrAny any;\n" +
									"        \n" +
									"        std::map<std::string,void*> result;\n" +
									"        any.get_local_func_addr_symtab(\"^foo()$\", result);\n" +
									"        Stub stub;\n" +
									"        std::map<std::string,void*>::iterator it_;\n" +
									"        for (it_=result.begin(); it_!=result.end(); ++it_)\n" +
									"        {\n" +
									"            stub.set(it_->second ,foo_stub);\n" +
									"            std::cout << it_->first << \" => \" << it_->second << std::endl;\n" +
									"        }";
							testCaseStubMainCode = testDriverTemplateMain.replace("any", any).replace("result", result).replace("stub", stub).replace("it_", it);


						} else {
							testCaseStubMainCode = "";

							testCaseStubMainCode += "Stub " + "stubOf" + function.getSimpleName() + ";\n";

							testCaseStubMainCode += "stubOf" + function.getSimpleName() + ".set(" + function.getSimpleName() + ", " + function.getSimpleName() + "_stub" + ");" + "\n";
						}
					}

//					String stubCode = getStubCode(function);
//					String stubCode = testCaseStubCode;
//					if (stubCode != null) {
//						if (isStubByTestCase(stubCode, testCaseName)) {
//							String prevTestCaseStubCode = getTestCaseStubCode(stubCode, testCaseName);
//							stubCode = stubCode.replace(prevTestCaseStubCode, testCaseStubCode);
//						} else {
//							stubCode += testCaseStubCode;
//						}
//
//						stubCode = stubCode.replaceAll("\n\n\n+", "\n\n");
//
//						Utils.writeContentToFile(stubCode, stubPath);
//					}

					stubMainCode += testCaseStubMainCode;
				}
			}
		} catch (Exception ex) {
			logger.error("Can't generate stub main code: " + ex.getMessage());
		}
		return stubMainCode;
	}

	private static String generateStubBodyFor(SubprogramNode subprogram) throws Exception {
		StringBuilder output = new StringBuilder();
		output.append(FILE_BANNER);
		IDataNode firstChild = subprogram.getChildren().get(0);
		if (firstChild instanceof NumberOfCallNode) {
			NumberOfCallNode numberofcallnode = (NumberOfCallNode) firstChild;
			List<IDataNode> calls = numberofcallnode.getChildren();

			for (IDataNode call : calls) {
				output.append(genIterSubprogramNodeBody((IterationSubprogramNode) call));
			}

			if (isVoid(subprogram)) {
				output.append("return;");
			}
		}

		return output.toString();
	}

	public static String generateStubCode(TestCase testCase) {
		List<SubprogramNode> stubSubprograms = Search2.searchStubableSubprograms(testCase.getRootDataNode());
//        if(stubSubprograms.size()>0) {
//            stubSubprograms.remove(stubSubprograms.size() - 1);
//        }
		String testCaseName = testCase.getName();
		String stubBodyCode = SpecialCharacter.EMPTY;
		try {

			for (SubprogramNode child : stubSubprograms) {
				if (child != null && !(child instanceof IterationSubprogramNode)) {
					SubprogramNode subprogram = child;

					INode function = subprogram.getFunctionNode();
					String stubPath = getStubCodeFilePath(function.getAbsolutePath());

					String testCaseStubCode = SpecialCharacter.EMPTY;

					if (subprogram.isStub())
						testCaseStubCode = generateStubCodeFor(testCaseName, subprogram);

					String stubCode = getStubCode(function);

					if (stubCode != null) {
						if (isStubByTestCase(stubCode, testCaseName)) {
							String prevTestCaseStubCode = getTestCaseStubCode(stubCode, testCaseName);
							stubCode = stubCode.replace(prevTestCaseStubCode, testCaseStubCode);
						} else {
							stubCode += testCaseStubCode;
						}

						stubCode = stubCode.replaceAll("\n\n\n+", "\n\n");

						Utils.writeContentToFile(stubCode, stubPath);
					}
					stubBodyCode += testCaseStubCode;
				}
			}
		} catch (Exception ex) {
			logger.error("Can't generate stub code: " + ex.getMessage());
		}
		return stubBodyCode;
	}

	private static String generateFunctionStub(SubprogramNode subprogram) {
		String code = SpecialCharacter.EMPTY;


		ICommonFunctionNode functionNode = (ICommonFunctionNode) subprogram.getFunctionNode();

		if (functionNode instanceof ConstructorNode) {
			return SpecialCharacter.EMPTY;
		}

		String returnType = functionNode.getReturnType().trim();
		code += returnType;
		code += SpecialCharacter.SPACE_STR;
		code += Utils.getFullFunctionCallForStub(functionNode);
		if (subprogram.getFunctionNode().getParent() instanceof ClassNode || subprogram.getFunctionNode().getParent() instanceof StructNode) {
			if (((ICommonFunctionNode) subprogram.getFunctionNode()).isStatic()) {

			} else {

				int check = 0;
				for (int i = 0; i < code.length(); i++) {
					if (code.charAt(i) == '(') {
						check = i;
					}
				}
				String code1 = code.substring(0, check + 1);
				String code2 = code.substring(check + 1);
				code = code1 + "void* obj" + code2;
			}
		}
		code += "\n";

		return code;
	}

	public static String generateStubCodeFor(String testCaseName, SubprogramNode subprogram) throws Exception {
		String code = SpecialCharacter.EMPTY;

		String body = generateStubBodyFor(subprogram);
		LocalDateTime updatedTime = LocalDateTime.now();
		String beginTag = BEGIN_STUB_CASE_TAG.replace("{{INSERT_TEST_CASE_NAME_HERE}}", testCaseName)
				+ BEGIN_LAST_UPDATED_TAG.replace("{{INSERT_LAST_UPDATED_HERE}}",
				DateTimeUtils.getDate(updatedTime) + " " + DateTimeUtils.getTime(updatedTime));
		String endTag = END_STUB_CASE_TAG.replace("{{INSERT_TEST_CASE_NAME_HERE}}", testCaseName);

		code += beginTag + "\n";
		String functionStub = generateFunctionStub(subprogram);
		code += functionStub;
		code += SpecialCharacter.OPEN_BRACE + "\n";
		if (subprogram.getFunctionNode().getParent() instanceof ClassNode || subprogram.getFunctionNode().getParent() instanceof StructNode) {
			if (((ICommonFunctionNode) subprogram.getFunctionNode()).isStatic()) {

			} else if (((ICommonFunctionNode) subprogram.getFunctionNode()).isVirtual()) {
				int beginIndex = functionStub.indexOf("(void* ") + 7;
				int endIndex = functionStub.indexOf(")");
				String obj = functionStub.substring(beginIndex, endIndex);
				code += subprogram.getFunctionNode().getParent().getName() + "* o = (" + subprogram.getFunctionNode().getParent().getName() + "*)" + obj + ";\n";
			}
			else {
				code += subprogram.getFunctionNode().getParent().getName() + "* o = (" + subprogram.getFunctionNode().getParent().getName() + "*)obj;\n";
			}
		}

		String defineName = testCaseName.toUpperCase()
				.replace(SpecialCharacter.DOT, SpecialCharacter.UNDERSCORE_CHAR);
		defineName = String.format("AKA_TC_%s\n", defineName);

//		code += "#ifdef " + defineName;

//		code += String.format("if (strcmp(%s, \"%s\") == 0) {\n%s\n}\n", AKA_TEST_CASE_NAME, testCaseName, body);

//		code += "#endif //" + defineName;

		code += body;
		code += "\n}\n";
		code += endTag + "\n";

		return code;
	}

	private static boolean isVoid(SubprogramNode subprogram) {
		String returnType = subprogram.getRealType();
		returnType = VariableTypeUtils.removeRedundantKeyword(returnType);

		return returnType.equals(VariableTypeUtils.VOID_TYPE.VOID);
	}

	private static boolean isOverloadMethod(SubprogramNode subprogram) {
		int count = 0;
		FunctionNode function = (FunctionNode) subprogram.getFunctionNode();
		ArrayList arrayList = (ArrayList) subprogram.getFunctionNode().getParent().getChildren();
		for (int i = 0; i < arrayList.size(); i++) {
			if (arrayList.get(i) instanceof FunctionNode) {

				FunctionNode arrListFunc = (FunctionNode) arrayList.get(i);
				if (function.getSimpleName().equals(arrListFunc.getSimpleName())) count++;
			}
		}
		if (count > 1) return true;
		return false;
	}

	public static void initializeStubCode(String functionName, String unitName, String functionPath, String filePath) {
		if (new File(filePath).exists()) {
			return;
		}

		final int MAX_LENGTH = 68;

		if (functionName.length() > MAX_LENGTH) {
			int pos = MAX_LENGTH;
			while (functionName.charAt(pos) != ',' && functionName.charAt(pos) != '(' && pos > 0)
				pos--;
			functionName = functionName.substring(0, pos) + "\n *\t\t\t\t\t\t\t" + functionName.substring(pos);
		}

		if (functionPath.length() > MAX_LENGTH) {
			int pos = MAX_LENGTH;
			while (functionPath.charAt(pos) != File.separatorChar && pos > 0)
				pos--;
			functionPath = functionPath.substring(0, pos) + "\n *\t\t\t\t\t\t\t" + functionPath.substring(pos);
		}

		functionPath = PathUtils.toRelative(functionPath);

		String fileBanner = String.format(FILE_BANNER, unitName, functionName, functionPath);

		Utils.writeContentToFile(fileBanner, filePath);

		logger.debug("Initialize stub code for " + functionName + " in " + unitName + " to " + filePath + " successfully");
	}

	public static void initializeStubCode(FunctionNode function) {
		String unit = Utils.getSourcecodeFile(function).getName();
		String name = function.getName();
		String path = function.getAbsolutePath();
		String filePath = FolderConfig.load().getStubCodeDirectory();

		if (!filePath.endsWith(File.separator))
			filePath += File.separator;
		filePath += getStubCodeFileName(function);

		initializeStubCode(name, unit, path, filePath);
	}

	public static boolean isStubByTestCase(String stubCode, String testCaseName) {
		String begin = BEGIN_STUB_CASE_TAG.replace("{{INSERT_TEST_CASE_NAME_HERE}}", testCaseName);
		String end = END_STUB_CASE_TAG.replace("{{INSERT_TEST_CASE_NAME_HERE}}", testCaseName);

		return stubCode.contains(begin) && stubCode.contains(end);
	}

	public static String getTestCaseStubCode(String stubCode, String testCaseName) {
		String begin = BEGIN_STUB_CASE_TAG.replace("{{INSERT_TEST_CASE_NAME_HERE}}", testCaseName);
		String end = END_STUB_CASE_TAG.replace("{{INSERT_TEST_CASE_NAME_HERE}}", testCaseName);

		int beginPos = stubCode.indexOf(begin);
		int endPos = stubCode.lastIndexOf(end) + end.length();

		return stubCode.substring(beginPos, endPos);
	}

	public static String getStubCode(INode functionNode) {
		String functionPath = functionNode.getAbsolutePath();
		String stubPath = getStubCodeFilePath(functionPath);
		if (stubPath == null)
			return null;

		return Utils.readFileContent(stubPath);
	}


	/**
	 * Get corresponding stub code file path
	 * <<Unit name>>.<<Subprogram>>.<<Offset>>.stub
	 *
	 * @param origin subprogram
	 * @return corresponding stub code file path
	 */
	public static String getStubCodeFileName(AbstractFunctionNode origin) {
		INode unit = Utils.getSourcecodeFile(origin);
		String unitName = unit.getName();
		String subprogramName = origin.getSimpleName()
				.replace(SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS, SpecialCharacter.DOT_IN_STRUCT);

		IASTFileLocation location = origin.getNodeLocation();
		int offset = location.getNodeOffset();

		return unitName + SpecialCharacter.DOT + subprogramName + SpecialCharacter.DOT
				+ offset + STUB_EXTENSION;
	}

	/**
	 * Get file list
	 */
	public static Map<String, String> getFileList() {
		if (fileList == null) {
			File stubsFile = new File(getStubFileListPath());
			if (stubsFile.exists())
				fileList = importStubFileList();
			else
				fileList = new HashMap<>();
		}

		return fileList;
	}

	/**
	 * Add stub file into list
	 */
	public static synchronized void addStubFile(String functionPath, String stubPath) {
		getFileList().put(functionPath, stubPath);
	}

	/**
	 * Get stub file path
	 *
	 * @param path of corresponding function node
	 * @return stub file path
	 */
	public static String getStubCodeFilePath(String path) {
		return getFileList().get(path);
	}

	/**
	 * Export list stub file
	 * function path -> stub file path
	 */
	public static void exportListToFile() {
		String path = getStubFileListPath();
		StringBuilder fileContent = new StringBuilder();

		fileList.forEach((k, v) -> fileContent
				.append(PathUtils.toRelative(k))
				.append(": ")
				.append(PathUtils.toRelative(v))
				.append(SpecialCharacter.LINE_BREAK));

		Utils.writeContentToFile(fileContent.toString(), path);
	}

	private static String getStubFileListPath() {
		//String path = new WorkspaceConfig().fromJson().getStubCodeDirectory();
		String path = FolderConfig.load().getStubCodeDirectory();
		if (!path.endsWith(File.separator))
			path += File.separator;
		path += STUBS_FILE_NAME;

		return path;
	}

	public static Map<String, String> importStubFileList() {
		Map<String, String> fileList = new HashMap<>();
		String path = getStubFileListPath();

		String content = Utils.readFileContent(path);
		String[] lines = content.split("\\R");

		for (String line : lines) {
			String[] pathItems = line.split(":\\s");
			if (pathItems.length == 2) {
				String source = PathUtils.toAbsolute(pathItems[0]);
				source = Utils.normalizePath(source);
				String stub = PathUtils.toAbsolute(pathItems[1]);
				stub = Utils.normalizePath(stub);

				fileList.put(source, stub);
			}
		}

		return fileList;
	}


	public String getDisplayName(String name) {
		int check = 0;
		for (int i = 0; i < name.length(); i++) {
			if (name.charAt(i) == '(') {
				check = i;
			}
		}
		name = name.substring(0, check);
		return name;
	}

	private static final String FILE_BANNER =
			"/** Static counting variable **/\n" +
					"static int AKA_INTRA_FCOUNT = 0;\n" +
					"AKA_INTRA_FCOUNT++;\n\n" +
					"/*******************************************************************************\n" +
					" * Stub code for subprogram in tested project.\n" +
					" * Initialize automatically by AKA.\n" +
					" *\n" +
					" * @unit: %s\n" +
					" * @name: %s\n" +
					" * @path: %s\n" +
					" ******************************************************************************/\n";

	private static final String BEGIN_STUB_CASE_TAG =
			"/**\n" +
					" * BEGIN AKA STUB CASE\n" +
					" * @name: {{INSERT_TEST_CASE_NAME_HERE}}\n";

	private static final String BEGIN_LAST_UPDATED_TAG =
			" *\n" +
					" * @last-updated: {{INSERT_LAST_UPDATED_HERE}}\n" +
					" */";

	private static final String END_STUB_CASE_TAG = "/** END AKA STUB CASE - {{INSERT_TEST_CASE_NAME_HERE}} */";

	public static final String STUB_EXTENSION = ".stub";

	public static final String STUBS_FILE_NAME = "STUBS.aka";

	public static boolean isCheckStaticNormalFunc() {
		return checkStaticNormalFunc;
	}

	public static void setCheckStaticNormalFunc(boolean checkStaticNormalFunc) {
		StubManager.checkStaticNormalFunc = checkStaticNormalFunc;
	}
}
