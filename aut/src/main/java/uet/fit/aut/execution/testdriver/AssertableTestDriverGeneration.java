package uet.fit.aut.execution.testdriver;

import uet.fit.aut.parser.obj.ConstructorNode;
import uet.fit.aut.search.Search2;
import uet.fit.aut.execution.DriverConstant;
import uet.fit.aut.stub_manager.StubManager;
import uet.fit.aut.testcase.TestCase;
import uet.fit.aut.testdata.comparable.AssertMethod;
import uet.fit.aut.testdata.object.*;
import uet.fit.aut.util.SourceConstant;
import uet.fit.aut.util.SpecialCharacter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class AssertableTestDriverGeneration extends TestDriverGeneration {

    protected final String LIB_STUB_PATH = "";
    protected String generateBodyScript(TestCase testCase) throws Exception {
        // STEP 1: assign aut test case name
        String testCaseNameAssign = String.format("%s=\"%s\";", DriverConstant.TEST_NAME, testCase.getName());

        // STEP 2: Generate initialization of variables
        String initialization = generateInitialization(testCase);

        //STEP 2.1 : Replace function to function_stub
        String stubMainCode = StubManager.generateStubMainCode(testCase);
//        stubMainCode = "";
        // STEP 3: Generate full function call
        String functionCall = generateFunctionCall(testCase);

        // STEP 4: FCALLS++ - Returned from UUT
        String increaseFcall;
        if (testCase.getFunctionNode() instanceof ConstructorNode)
            increaseFcall = SpecialCharacter.EMPTY;
        else
            increaseFcall = SourceConstant.INCREASE_FCALLS + generateReturnMark(testCase);

        // STEP 5: Generation assertion actual & expected values
        String assertion = generateAssertion(testCase);

        String beginMark = String.format("std::string begin = \"BEGIN OF %s\";\n%s(begin);\n", testCase.getName(), DriverConstant.MARK);
        String endMark = String.format("std::string end = \"END OF %s\";\n%s(end);\n", testCase.getName(), DriverConstant.MARK);

        // STEP 6: Repeat iterator
        String singleScript = String.format(
                    "{\n" +
                        "%s\n" +
                        "%s\n" +
                        "%s\n" +
                        "%s\n" +
                        "%s\n" +
                        "%s\n" +
                        "%s\n" +
                        "%s\n" +
                        "%s\n" +
                        "%s\n" +
                    "}",
                beginMark,
                testCaseNameAssign,
                testCase.getTestCaseUserCode().getSetUpContent(),
                initialization,
                stubMainCode,
                functionCall,
                increaseFcall,
                assertion,
                testCase.getTestCaseUserCode().getTearDownContent(),
                endMark
        );

//        StringBuilder script = new StringBuilder();
//        for (int i = 0; i < iterator; i++)
//            script.append(singleScript).append(SpecialCharacter.LINE_BREAK);

        // STEP 7: mark beginning and end of test case
//        script = new StringBuilder(wrapScriptInMark(testCase, script.toString()));
//        script = new StringBuilder(wrapScriptInTryCatch(script.toString()));
//
//        return script.toString();
        singleScript = wrapScriptInTryCatch(singleScript);

        return singleScript;
    }

    protected String generateAssertion(TestCase testCase) {
        String assertion = "/* error assertion */";

        IValueDataNode expectedOutputDataNode = Search2.getExpectedOutputNode(testCase.getRootDataNode());

        // not void function
        if (expectedOutputDataNode != null) {
//            if (expectedOutputDataNode.getRawType().equals("void*")){
//                assertion = "/*Does not support CU_ASSERT for void pointer comparison*/";
//            } else {
                assertion = expectedOutputDataNode.getAssertion();
//            }
        }

        // expected values
        assertion += generateExpectedValueInitialize(testCase);

        return assertion;
    }

    private String generateExpectedValueInitialize(TestCase testCase) {
        String initialize = "\n/* error expected initialize */";

        SubprogramNode sut = Search2.findSubprogramUnderTest(testCase.getRootDataNode());

        Map<ValueDataNode, ValueDataNode> globalExpectedMap = testCase.getGlobalInputExpOutputMap();

        if (sut != null) {
            initialize = SpecialCharacter.LINE_BREAK;

            List<ValueDataNode> expectedGlobals = new ArrayList<>(globalExpectedMap.values());

            try {
                // sut arguments
                initialize += addParamAssert(sut, true);

                // global variables
                for (ValueDataNode expected : expectedGlobals) {
                    boolean shouldInit = shouldInitializeExpected(expected);
                    boolean haveMethod = unnecessaryInitializeExpected(expected);
                    if (shouldInit || haveMethod) {
                        initialize += expected.getInputForGoogleTest(false);
                        initialize += SpecialCharacter.LINE_BREAK;
                        initialize += expected.getAssertion();
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return initialize;
    }

    private String addParamAssert(IDataNode current, boolean isFirstLevel) throws Exception {
        StringBuilder output = new StringBuilder();

        List<ValueDataNode> parameters;

        if (isFirstLevel && current instanceof SubprogramNode) {
            parameters = Search2.searchParameterNodes((SubprogramNode) current);
        } else {
            parameters = current.getChildren().stream()
                    .filter(n -> n instanceof ValueDataNode)
                    .map(n -> (ValueDataNode) n)
                    .collect(Collectors.toList());
        }

        for (ValueDataNode param : parameters) {
            boolean haveMethod = unnecessaryInitializeExpected(param);

            ValueDataNode expected = Search2.getExpectedValue(param);

            if (isFirstLevel) {
                if (param.getName().equals("RETURN"))
                    continue;

                if (expected != null) {
                    if (shouldInitializeExpected(expected)) {
                        output.append(expected.getInputForGoogleTest(false));
                        output.append(SpecialCharacter.LINE_BREAK);
                    }
                }
            }

            if (param.getAssertMethod() != null) {
                if (haveMethod) {
                    output.append(param.getAssertion());
                } else {
                    if (expected != null) {
                        output.append(expected.getAssertion());
                    }
                }
            }

            output.append(addParamAssert(param, false));
        }

        return output.toString();
    }

    private boolean unnecessaryInitializeExpected(ValueDataNode dataNode) {
        if (dataNode.getAssertMethod() == null)
            return false;

        String assertMethod = dataNode.getAssertMethod();

        return assertMethod.equals(AssertMethod.ASSERT_TRUE)
                || assertMethod.equals(AssertMethod.ASSERT_FALSE)
                || assertMethod.equals(AssertMethod.ASSERT_NULL)
                || assertMethod.equals(AssertMethod.ASSERT_NOT_NULL)
                || assertMethod.equals(AssertMethod.USER_CODE);
    }

    private boolean shouldInitializeExpected(ValueDataNode dataNode) {
        if (dataNode.isUseUserCode())
            return true;

        if (dataNode instanceof ArrayDataNode)
            return ((ArrayDataNode) dataNode).isSetSize();

        if (dataNode instanceof PointerDataNode)
            return ((PointerDataNode) dataNode).isSetSize();

        if (dataNode instanceof NormalDataNode)
            return ((NormalDataNode) dataNode).getValue() != null;

        if (dataNode instanceof ClassDataNode) {
            SubClassDataNode subClass = ((ClassDataNode) dataNode).getSubClass();

            if (subClass == null)
                return false;

            ConstructorDataNode constructor = subClass.getConstructorDataNode();

            if (constructor == null)
                return false;

            if (constructor.getChildren().size() == 0)
                return false;

            for (IDataNode argument : constructor.getChildren()) {
                if (!shouldInitializeExpected((ValueDataNode) argument))
                    return false;
            }

            return true;
        }

        if (dataNode instanceof EnumDataNode) {
            ((EnumDataNode) dataNode).getValue();
            return true;
        }

        return true;
    }
}
