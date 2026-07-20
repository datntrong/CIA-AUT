package uet.fit.aut.execution.testdriver;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.config.FolderConfig;
import uet.fit.aut.execution.DriverConstant;
import uet.fit.aut.instrument.ProjectClone;
import uet.fit.aut.logger.Locations;
import uet.fit.aut.parser.finder.Level;
import uet.fit.aut.parser.finder.VariableSearchingSpace;
import uet.fit.aut.parser.obj.AbstractFunctionNode;
import uet.fit.aut.parser.obj.ConstructorNode;
import uet.fit.aut.parser.obj.DestructorNode;
import uet.fit.aut.parser.obj.FunctionNode;
import uet.fit.aut.parser.obj.ICommonFunctionNode;
import uet.fit.aut.parser.obj.IFunctionNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.parser.obj.ISourcecodeFileNode;
import uet.fit.aut.parser.obj.InstanceVariableNode;
import uet.fit.aut.parser.obj.LambdaFunctionNode;
import uet.fit.aut.parser.obj.MacroFunctionNode;
import uet.fit.aut.parser.obj.NamespaceNode;
import uet.fit.aut.parser.obj.SourcecodeFileNode;
import uet.fit.aut.parser.obj.VariableNode;
import uet.fit.aut.search.Search;
import uet.fit.aut.search.Search2;
import uet.fit.aut.stub_manager.StubManager;
import uet.fit.aut.testcase.ITestCase;
import uet.fit.aut.testcase.TestCase;
import uet.fit.aut.testdata.object.ClassDataNode;
import uet.fit.aut.testdata.object.IDataNode;
import uet.fit.aut.testdata.object.RootDataNode;
import uet.fit.aut.testdata.object.StructDataNode;
import uet.fit.aut.testdata.object.SubprogramNode;
import uet.fit.aut.testdata.object.ValueDataNode;
import uet.fit.aut.util.PathUtils;
import uet.fit.aut.util.ResourceFileUtils;
import uet.fit.aut.util.SourceConstant;
import uet.fit.aut.util.SpecialCharacter;
import uet.fit.aut.util.Utils;
import uet.fit.aut.util.VariableTypeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static uet.fit.aut.instrument.ProjectClone.wrapInIncludeGuard;

/**
 * Generate test driver for a function
 *
 * @author Lamnt
 */
public abstract class TestDriverGeneration extends DriverGeneration implements ITestDriverGeneration {

    protected final static Logger logger = LoggerFactory.getLogger(TestDriverGeneration.class);

    protected List<String> testScripts;

    protected ITestCase testCase;

    protected String name;

    protected String testPathFilePath;

    protected List<String> clonedFilePaths;

    protected String environmentPath;

    protected File originProject;

    protected final String LIB_STUB_PATH = FolderConfig.load().getWorkspace() + "/cpp-stub";

    @Override
    public void generate() throws Exception {
        testPathFilePath = testCase.getTestPathFile();

        testScripts = new ArrayList<>();
        clonedFilePaths = new ArrayList<>();

		String stub_h_path = "#include \"" + LIB_STUB_PATH + "/src/stub.h\"\n";
		stub_h_path += "#include \"" + LIB_STUB_PATH + "/src/addr_pri.h\"\n";
		stub_h_path += "#include \"" + LIB_STUB_PATH + "/src_linux/addr_any.h\"";

		String stubCode = StubManager.generateStubCode((TestCase) testCase);
//        stubCode = "";

		boolean isFileExist = new File(LIB_STUB_PATH).exists();
		logger.debug("Stub lib exist = " + isFileExist);
		if (!isFileExist) {
			ResourceFileUtils.copyAndExtract(LIB_STUB_PATH, Locations.STUB_LIB_PATH);
			logger.debug("Copy stub lib successfully");
		}

		if (testCase instanceof TestCase) {
			String script = generateTestScript((TestCase) testCase);
			testScripts.add(script);
		}

//        else if (testCase instanceof CompoundTestCase) {
//            List<TestCaseSlot> slots = ((CompoundTestCase) testCase).getSlots();
//            for (TestCaseSlot slot : slots) {
//                String name = slot.getTestcaseName();
//                TestCase element = TestCaseManager.getBasicTestCaseByName(name);
//                assert element != null;
//                String testScript = generateTestScript(element);
//                testScripts.add(testScript);
//            }
//        }


		StringBuilder testScriptPart = new StringBuilder();
		for (String item : testScripts) {
			testScriptPart.append(item).append(SpecialCharacter.LINE_BREAK);
		}


		String includedPart = generateIncludePaths();
		String additionalIncludes = generateAdditionalHeaders();

		testDriver = getTestDriverTemplate()
				.replace(INCLUDE_LIB_TAG, stub_h_path)
				.replace(TEST_PATH_TAG, PathUtils.doubleNormalize(testPathFilePath))
				.replace(CLONED_SOURCE_FILE_PATH_TAG, includedPart)
				.replace(TEST_SCRIPTS_STUB_TAG, stubCode)
				.replace(TEST_SCRIPTS_TAG, testScriptPart.toString())
				.replace(ADDITIONAL_HEADERS_TAG, additionalIncludes)
				.replace(EXEC_TRACE_FILE_TAG, PathUtils.doubleNormalize(testCase.getExecutionResultTrace()))
				.replace(DriverConstant.ADD_TESTS_TAG, generateAddTestStm(testCase))
				.replace(TEST_DRIVER_NAME, name + ".moc");

//        if (testCase instanceof CompoundTestCase) {
//            TestCaseUserCode userCode = testCase.getTestCaseUserCode();
//            testDriver = testDriver
//                    .replace(COMPOUND_TEST_CASE_SETUP, userCode.getSetUpContent())
//                    .replace(COMPOUND_TEST_CASE_TEARDOWN, userCode.getTearDownContent());
//        }
	}

	protected String generateTestScript(TestCase testCase) throws Exception {
		String body = generateBodyScript(testCase);
		return String.format("void " + AUT_TEST_MAIN_PREFIX + "() {\n%s\n}\n", body);
	}

	protected static final String AUT_TEST_PREFIX = "AUT_TEST_";
	protected static final String AUT_TEST_MAIN_PREFIX = "TestDriver::mainTest";

	private String generateAddTestStm(ITestCase testCase) {
		StringBuilder out = new StringBuilder();

		if (testCase instanceof TestCase) {
			String runStm = generateRunStatement((TestCase) testCase);
			out.append(runStm);
		}
//        else if (testCase instanceof CompoundTestCase) {
//            List<TestCaseSlot> slots = ((CompoundTestCase) testCase).getSlots();
//            for (TestCaseSlot slot : slots) {
//                String name = slot.getTestcaseName();
//                int iterator = slot.getNumberOfIterations();
//                TestCase element = TestCaseManager.getBasicTestCaseByName(name);
//                if (element != null) {
//                    String runStm = generateRunStatement(element, iterator);
//                    out.append(runStm);
//                }
//            }
//        }

		return out.toString();
	}

	private String generateRunStatement(TestCase testCase) {
		String testCaseName = testCase.getName();
		String testName = testCaseName.toUpperCase();
		testCaseName = testCaseName.replaceAll("\\W", SpecialCharacter.UNDERSCORE);
		String test = AUT_TEST_PREFIX + testCaseName;
		return String.format(RUN_FORMAT, testName, test, 1);
	}

	private static final String RUN_FORMAT = "\t" + DriverConstant.RUN_TEST + "(\"%s\", &%s, %d);\n";

	private String generateAdditionalHeaders() {
		StringBuilder builder = new StringBuilder();

		if (testCase.getAdditionalHeaders() != null) {
			builder.append(testCase.getAdditionalHeaders()).append(SpecialCharacter.LINE_BREAK);
		}

        List<String> userCodeList = testCase.getAdditionalIncludes();
        for (String item : userCodeList) {
            String stm = String.format("#include \"%s\"\n", item);
            builder.append(stm);
        }

        if (testCase instanceof TestCase) {
            ICommonFunctionNode fn = testCase.getFunctionNode();
            ISourcecodeFileNode fileNode = Utils.getSourcecodeFile(fn);
            List<Level> space = new VariableSearchingSpace(fn).getSpaces();
            Set<String> listInSpace = new HashSet<>();
            for (Level l : space) {
                for (INode n : l) {
                    if (n instanceof ISourcecodeFileNode && !n.equals(fileNode)) {
                        listInSpace.add(n.getAbsolutePath());
                    }
                }
            }
            Set<String> additionalSources = ((TestCase) testCase).getRootDataNode().getAdditionalSources();
            List<String> finalList = new ArrayList<>(additionalSources);
            for (String item : additionalSources) {
                if (listInSpace.contains(item))
                    finalList.remove(item);
            }

            for (String originPath : finalList) {
			    String clonedPath = ProjectClone.getClonedFilePath(environmentPath, originProject, originPath);
                String stm = String.format("#include \"%s\"\n", clonedPath);
                builder.append(stm);
            }
        }

        return builder.toString();
    }

	protected String getCloneSourceCodeFile(TestCase testcase) {
		ICommonFunctionNode sut = testcase.getFunctionNode();
		ISourcecodeFileNode unit = Utils.getSourcecodeFile(sut);
		String originPath = unit.getAbsolutePath();
		return ProjectClone.getClonedFilePath(environmentPath, originProject, originPath);
	}

	protected String generateIncludePaths() {
		StringBuilder includedPart = new StringBuilder();

		if (testCase instanceof TestCase) {
			String path = getCloneSourceCodeFile((TestCase) testCase);
			clonedFilePaths.add(path);

			String includeClonedFile = String.format("#include \"%s\"\n", path);
			includedPart.append(includeClonedFile);

			String instanceDeclaration = generateInstanceDeclaration((TestCase) testCase);
			includedPart.append(instanceDeclaration);

//            if (!Environment.getInstance().isC()) {
			ICommonFunctionNode sut = ((TestCase) testCase).getRootDataNode().getFunctionNode();

			if (sut instanceof AbstractFunctionNode) {
				INode realParent = ((AbstractFunctionNode) sut).getRealParent();
				if (realParent == null) realParent = sut.getParent();

				while (!(realParent instanceof SourcecodeFileNode)) {
					if (realParent instanceof NamespaceNode)
						break;

					realParent = realParent.getParent();
				}

				while (realParent instanceof NamespaceNode) {
					includedPart.append(SpecialCharacter.LINE_BREAK);
					String namespace = Search.getScopeQualifier(realParent);
					String usingNamespace = String.format("using namespace %s;\n", namespace);
					includedPart.append(usingNamespace);
					realParent = realParent.getParent();
				}
//                }
			}

		}
//        else if (testCase instanceof CompoundTestCase) {
//            List<TestCaseSlot> slots = ((CompoundTestCase) testCase).getSlots();
//
//            for (TestCaseSlot slot : slots) {
//                String name = slot.getTestcaseName();
//                TestCase element = TestCaseManager.getBasicTestCaseByName(name);
//
//                assert element != null;
//                String clonedFilePath = getCloneSourceCodeFile(element);
//
//                if (!clonedFilePaths.contains(clonedFilePath)) {
//                    clonedFilePaths.add(clonedFilePath);
//
//                    String path = PathUtils.doubleNormalize(clonedFilePath);
//                    String includeClonedFile = String.format("#include \"%s\"\n", path);
//                    includedPart.append(includeClonedFile);
//
//                    String instanceDeclaration = generateInstanceDeclaration(element);
//                    includedPart.append(instanceDeclaration);
//                }
//            }
//        }

		return includedPart.toString();
	}

	protected String generateBodyScript(TestCase testCase) throws Exception {
		// STEP 1: assign aut test case name
		String testCaseNameAssign = String.format("%s=\"%s\";", DriverConstant.TEST_NAME, testCase.getName());

		// STEP 2: Generate initialization of variables
		String initialization = generateInitialization(testCase);

		//STEP 2.1 : Replace function to function_stub
		String StubMainCode = StubManager.generateStubMainCode(testCase);

		// STEP 3: Generate full function call
		String functionCall = generateFunctionCall(testCase);

		// STEP 4: FCALLS++ - Returned from UUT
		String increaseFcall;
		if (testCase.getFunctionNode() instanceof ConstructorNode)
			increaseFcall = SpecialCharacter.EMPTY;
		else
			increaseFcall = SourceConstant.INCREASE_FCALLS + generateReturnMark(testCase);


		// STEP 5: Repeat iterator
		String singleScript = String.format(
				"{\n" +
						"%s\n" +
						"%s\n" +
						"%s\n" +
						"%s\n" +
						"%s\n" +
						"%s\n" +
						"}",
				testCaseNameAssign,
				testCase.getTestCaseUserCode().getSetUpContent(),
				initialization,
				functionCall,
				increaseFcall,
				testCase.getTestCaseUserCode().getTearDownContent());

//        StringBuilder script = new StringBuilder();
//        for (int i = 0; i < iterator; i++)
//            script.append(singleScript).append(SpecialCharacter.LINE_BREAK);

		// STEP 6: mark beginning and end of test case
//        script = new StringBuilder(wrapScriptInMark(testCase, script.toString()));
//        script = new StringBuilder(wrapScriptInTryCatch(script.toString()));
//
//        return script.toString();
		singleScript = wrapScriptInTryCatch(singleScript);
		return singleScript;
	}

	protected String generateReturnMark(TestCase testCase) {
		ICommonFunctionNode sut = testCase.getFunctionNode();

		String markStm;

		String functionPath = PathUtils.doubleNormalize(sut.getAbsolutePath());

		if (sut instanceof FunctionNode || sut instanceof MacroFunctionNode || sut instanceof LambdaFunctionNode) {
			markStm = String.format(DriverConstant.MARK + "(\"Return from: %s\");", functionPath);
		} else {
			SubprogramNode subprogram = null;

			INode parent = sut.getParent();

			if (sut instanceof IFunctionNode && ((IFunctionNode) sut).getRealParent() != null)
				parent = ((IFunctionNode) sut).getRealParent();

			RootDataNode globalRoot = Search2.findGlobalRoot(testCase.getRootDataNode());

			assert globalRoot != null;
			for (IDataNode globalVar : globalRoot.getChildren()) {
				if (((ValueDataNode) globalVar).getCorrespondingVar() instanceof InstanceVariableNode
						&& ((ValueDataNode) globalVar).getCorrespondingType().equals(parent)
						&& !globalVar.getChildren().isEmpty()
						&& !globalVar.getChildren().get(0).getChildren().isEmpty()) {

					subprogram = (SubprogramNode) globalVar.getChildren().get(0).getChildren().get(0);
				}
			}

			assert subprogram != null;

			String subprogramPath = subprogram.getPathFromRoot();
			markStm = String.format(DriverConstant.MARK + "(\"Return from: %s|%s\");", functionPath, subprogramPath);
		}

		return markStm;
	}

	protected abstract String wrapScriptInTryCatch(String script);

	private String generateInstanceDeclaration(TestCase testCase) {
//        if (Environment.getInstance().isC())
//            return SpecialCharacter.EMPTY;

		RootDataNode root = testCase.getRootDataNode();
		IDataNode globalRoot = Search2.findGlobalRoot(root);

		// get sut real parent
		ICommonFunctionNode sut = root.getFunctionNode();
		INode realParent = sut.getParent();
		if (sut instanceof AbstractFunctionNode) {
			realParent = ((AbstractFunctionNode) sut).getRealParent();
			if (realParent == null) realParent = sut.getParent();
		}

		StringBuilder init = new StringBuilder();

		if (globalRoot != null) {
			for (IDataNode child : globalRoot.getChildren()) {
				if (child instanceof ValueDataNode) {
					VariableNode varNode = ((ValueDataNode) child).getCorrespondingVar();
					INode varTypeNode = varNode.getCorrespondingNode();

					if (varNode instanceof InstanceVariableNode
							&& (varTypeNode == realParent
							|| child instanceof StructDataNode
							|| (child instanceof ClassDataNode
							&& !child.getChildren().isEmpty()))
					) {
						String type = varNode.getRawType();
						String name = child.getVirtualName();
						String instanceDefinition = String.format("%s* %s;", type, name);

						init.append(wrapInIncludeGuard(SourceConstant.GLOBAL_PREFIX + name, instanceDefinition));
					}
				}
			}
		}

		return init.toString();
	}

	protected String generateInitialization(TestCase testCase) throws Exception {
		String initialization = "";

		RootDataNode root = testCase.getRootDataNode();
		IDataNode globalRoot = Search2.findGlobalRoot(root);
		IDataNode sutRoot = Search2.findSubprogramUnderTest(root);

		if (globalRoot != null) {
//            List <IDataNode> bfsList = new ArrayList<>() ;
//            bfsList.addAll(globalRoot.getChildren());
//            while(!bfsList.isEmpty()){
//                IDataNode child = bfsList.get(0);
//                if (Environment.getInstance().isC()
//                        && child instanceof ValueDataNode
//                        && ((ValueDataNode) child).getCorrespondingVar() instanceof InstanceVariableNode) {
//                    continue;
//                }
//                initialization += child.getInputForGoogleTest();
//                bfsList.remove(bfsList.get(0));
//                bfsList.addAll(child.getChildren());
//            }
			for (IDataNode child : globalRoot.getChildren()) {
				if (
//                        Environment.getInstance().isC() &&
						child instanceof ValueDataNode &&
								((ValueDataNode) child).getCorrespondingVar() instanceof InstanceVariableNode) {
//                    continue;
				}

				initialization += child.getInputForGoogleTest(false);
			}
		}

		if (sutRoot == null)
			initialization = "/* error initialization */";
		else {

			for (IDataNode child : sutRoot.getChildren()) {
				initialization += child.getInputForGoogleTest(true);
			}
			initialization += sutRoot.getInputForGoogleTest(false);
		}

		initialization = initialization.replace(DriverConstant.MARK + "(\"<<PRE-CALLING>>\");",
				String.format(DriverConstant.MARK + "(\"<<PRE-CALLING>> Test %s\");", testCase.getName()));

		initialization = initialization.replaceAll("\\bconst\\s+\\b", SpecialCharacter.EMPTY);

		return initialization;
	}

	protected String generateFunctionCall(TestCase testCase) {
		ICommonFunctionNode functionNode = testCase.getFunctionNode();

		String functionCall = SpecialCharacter.EMPTY;

		if (functionNode instanceof ConstructorNode) {
			return SpecialCharacter.EMPTY;
		}

		String returnType = functionNode.getReturnType().trim();
		returnType = VariableTypeUtils.deleteVirtualAndInlineKeyword(returnType);
		returnType = VariableTypeUtils.deleteStorageClassesExceptConst(returnType);

		if (functionNode instanceof MacroFunctionNode || functionNode.isTemplate()) {
			SubprogramNode sut = Search2.findSubprogramUnderTest(testCase.getRootDataNode());

			if (sut != null)
				returnType = sut.getRawType();
		}

		if (functionNode instanceof DestructorNode) {
			functionCall = Utils.getFullFunctionCall(functionNode);

		} else if (!returnType.equals(VariableTypeUtils.VOID_TYPE.VOID)) {
			functionCall = returnType + " " + SourceConstant.ACTUAL_OUTPUT;

			functionCall += "=" + Utils.getFullFunctionCall(functionNode);
		} else
			functionCall = Utils.getFullFunctionCall(functionNode);

		functionCall = functionCall.replaceAll(ProjectClone.MAIN_REGEX, ProjectClone.MAIN_REFACTOR_NAME);

		functionCall = String.format(DriverConstant.MARK + "(\"<<PRE-CALLING>> Test %s\");%s", testCase.getName(), functionCall);

		return functionCall;
	}

	public void setTestCase(ITestCase testCase) {
		this.testCase = testCase;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setOriginProject(String originProject) {
		this.originProject = new File(originProject);
	}

	public void setEnvironmentPath(String environmentPath) {
		this.environmentPath = environmentPath;
	}
}
