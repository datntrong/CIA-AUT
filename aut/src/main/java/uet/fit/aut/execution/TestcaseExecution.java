//package uet.fit.aut.testcase_execution;
//
//import uet.fit.aut.autogen.testdatagen.AbstractAutomatedTestdataGeneration;
//import uet.fit.aut.env.Environment;
//import uet.fit.aut.logger.AUTLogger;
//import uet.fit.aut.parser.object.ICommonFunctionNode;
//import uet.fit.aut.stub_manager.StubManager;
//import uet.fit.aut.testcase_execution.testdriver.*;
//import uet.fit.aut.testcase_manager.ITestCase;
//import uet.fit.aut.testcase_manager.TestCase;
//import uet.fit.aut.util.Utils;
//
//import java.io.File;
//
//import static uet.fit.aut.testcase_manager.ITestCase.STATUS_RUNTIME_ERR;
//
///**
// * Execute a test case
// */
//public class TestcaseExecution extends AbstractTestcaseExecution {
//    /**
//     * node corresponding with subprogram under test
//     */
//    private ICommonFunctionNode function;
//
//    private final static AUTLogger logger = AUTLogger.get(AbstractAutomatedTestdataGeneration.class);
//
//    @Override
//    public void execute() throws Exception {
//        if (!(getTestCase() instanceof TestCase)) {
//            logger.debug(getTestCase().getName() + " is not a normal test case");
//            return;
//        }
//
//        TestCase testCase = (TestCase) getTestCase();
//
//        initializeConfigurationOfTestcase(testCase);
//        logger.debug("Start generating test driver for the test case " + getTestCase().getPath());
//
//        // create the right version of test driver generation
////        UILogger.getUiLogger().log("Generating test driver of test case " + testCase.getPath());
//        testDriverGen = generateTestDriver(testCase);
//
//        if (testDriverGen != null) {
//            if (getMode() != IN_AUTOMATED_TESTDATA_GENERATION_MODE) {
////                UILogger.getUiLogger().log("Generating stub code of test case " + testCase.getPath());
//                StubManager.generateStubCode(testCase);
//            }
//
//            CommandConfig testCaseCommandConfig = new CommandConfig().fromJson(testCase.getCommandConfigFile());
//
//            IDriverGenMessage compileAndLinkMessage = compileAndLink(testCaseCommandConfig);
////            logger.debug(String.format("Compile & Link Message:\n%s\n", compileAndLinkMessage));
//
//            // Run the executable file
//            if (new File(testCase.getExecutableFile()).exists()) {
//                logger.debug("Execute " + testCase.getExecutableFile());
//
//                String message = runExecutableFile(testCaseCommandConfig);
//                testCase.setExecuteLog(message);
//
//                logger.debug("Execute done");
//
//                if (getMode() == IN_DEBUG_MODE) {
//                    // nothing to do
//                } else {
//                    if (new File(testCase.getTestPathFile()).exists()) {
//                        refactorResultTrace(testCase);
//                        boolean completed = analyzeTestpathFile(testCase);
//
//                        if (!completed) {
//                            String msg = "Runtime error " + testCase.getExecutableFile();
//                            logger.debug(msg);
////                            if (/*getMode() == IN_EXECUTION_WITHOUT_GTEST_MODE
////                                ||*/ getMode() == IN_EXECUTION_WITH_FRAMEWORK_TESTING_MODE) {
//                                testCase.setStatus(STATUS_RUNTIME_ERR);
////                                TestCaseManager.exportBasicTestCaseToFile(testCase);
//                                return;
////                            }
//                        }
//
//                    } else {
//                        String msg = "Does not found the test path file when executing " + testCase.getExecutableFile();
//                        logger.debug(msg);
//                        if (/*getMode() == IN_EXECUTION_WITHOUT_GTEST_MODE
//                                ||*/ getMode() == IN_EXECUTION_WITH_FRAMEWORK_TESTING_MODE) {
//                            UIController.showErrorDialog(msg, "Test case execution", "Fail");
////                            UILogger.getUiLogger().log("Execute " + testCase.getPath() + " failed.\nMessage = " + msg);
//                            testCase.setStatus(TestCase.STATUS_FAILED);
//                            return;
//                        }
//                        //throw new Exception(msg);
//                    }
//                }
//            } else {
//                String msg = "Can not generate executable file " + testCase.getFunctionNode().getAbsolutePath() + "\nError:" + compileAndLinkMessage;
//                logger.debug(msg);
//
//                if (getMode() == IN_EXECUTION_WITH_FRAMEWORK_TESTING_MODE) {
//                    final String title = "Executable file generation";
//                    String headText, content;
//                    if (!compileAndLinkMessage.isCompileSuccess()) {
//                        headText = "Failed to compile test driver";
//                        content = compileAndLinkMessage.getCompileMessage();
//                    } else {
//                        headText = "Failed to link test driver";
//                        content = compileAndLinkMessage.getLinkMessage();
//                    }
//
//                    UIController.showDetailDialog(Alert.AlertType.ERROR, title, headText, content);
//
//                } else if (getMode() == IN_AUTOMATED_TESTDATA_GENERATION_MODE) {
//                    // do nothing
//                }
//                if (getMode() != IN_DEBUG_MODE) {
//                    testCase.setStatus(TestCase.STATUS_FAILED);
//                }
//                return;
//            }
//
//        } else {
//            String msg = "Can not generate test driver of the test case for the function "
//                    + testCase.getFunctionNode().getAbsolutePath();
//            logger.debug(msg);
//            if (/*getMode() == IN_EXECUTION_WITHOUT_GTEST_MODE
//                    ||*/ getMode() == IN_EXECUTION_WITH_FRAMEWORK_TESTING_MODE) {
//                UIController.showErrorDialog(msg, "Test driver generation", "Fail");
////                UILogger.getUiLogger().log("Can not generate test driver for " + testCase.getPath() + ".\nMessage = " + msg);
//                if (getMode() != IN_DEBUG_MODE) {
//                    testCase.setStatus(TestCase.STATUS_FAILED);
//                }
//                return;
//            }
////            throw new Exception(msg);
//        }
//        if (getMode() != IN_DEBUG_MODE) {
//            testCase.setStatus(TestCase.STATUS_SUCCESS);
//        }
//    }
//
//    public TestDriverGeneration generateTestDriver(ITestCase testCase) throws Exception {
//        TestDriverGeneration testDriver = null;
//
//        // create the right version of test driver generation
//        switch (getMode()) {
//            case IN_AUTOMATED_TESTDATA_GENERATION_MODE:
//
//            case IN_EXECUTION_WITH_FRAMEWORK_TESTING_MODE:
//                /*case IN_EXECUTION_WITHOUT_GTEST_MODE: */{
//                initializeCommandConfigToRunTestCase(testCase);
//                testDriver = new TestDriverGenerationForCpp();
//                break;
//            }//testDriver = new TestDriverGenerationforCWithGoogleTest();
//
//            case IN_DEBUG_MODE: {
//                initializeCommandConfigToRunTestCase(testCase);
//                testDriver = new TestDriverGenerationForCppDebugger();
//                break;
//            }
//        }
//
//        if (testDriver != null) {
//            // generate test driver
//            testDriver.setTestCase(testCase);
//            testDriver.generate();
//            String testdriverContent = testDriver.getTestDriver();
//
////            if (testCase.getAdditionalHeaders() != null && testCase.getAdditionalHeaders().length() > 0) {
////                testdriverContent = testdriverContent.replace(ITestDriverGeneration.ADDITIONAL_HEADERS, testCase.getAdditionalHeaders());
//                Utils.writeContentToFile(testdriverContent, testCase.getSourceCodeFile());
////            } else {
////                testdriverContent = testdriverContent.replace(ITestDriverGeneration.ADDITIONAL_HEADERS, "");
////                Utils.writeContentToFile(testdriverContent, testCase.getSourceCodeFile());
////            }
//            logger.debug("Add test driver to " + testCase.getSourceCodeFile() + " done");
//        }
//
//        return testDriver;
//    }
//
//    public ICommonFunctionNode getFunction() {
//        return function;
//    }
//
//    public void setFunction(ICommonFunctionNode function) {
//        this.function = function;
//    }
//}