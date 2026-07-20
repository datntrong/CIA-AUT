//package uet.fit.aut.testcase_execution;
//
//import uet.fit.aut.autogen.cfg.testpath.ITestpathInCFG;
//import uet.fit.aut.autogen.instrument.FunctionInstrumentationForStatementvsBranch_Markerv2;
//import uet.fit.aut.autogen.testdata.object.TestpathString_Marker;
//import uet.fit.aut.compiler.Terminal;
//import uet.fit.aut.config.AkaConfig;
//import uet.fit.aut.config.CommandConfig;
//import uet.fit.aut.config.WorkspaceConfig;
//import uet.fit.aut.coverage.SourcecodeCoverageComputation;
//import uet.fit.aut.coverage.highlight.SourcecodeHighlighterForCoverage;
//import uet.fit.aut.env.Environment;
//import uet.fit.aut.parser.object.ISourcecodeFileNode;
//import uet.fit.aut.project_init.ProjectClone;
//import uet.fit.aut.testcase_execution.testdriver.TestDriverGeneration;
//import uet.fit.aut.testcase_manager.ITestCase;
//import uet.fit.aut.testcase_manager.TestCase;
//import uet.fit.aut.testcase_manager.TestCaseManager;
//import uet.fit.aut.util.*;
//import javafx.application.Platform;
//import javafx.scene.control.Alert;
//
//import java.io.File;
//import java.io.IOException;
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.TimeUnit;
//
//public abstract class AbstractTestcaseExecution implements ITestcaseExecution {
//    protected TestDriverGeneration testDriverGen;
//    private int mode = IN_EXECUTION_WITH_FRAMEWORK_TESTING_MODE; // by default
//    private ITestCase testCase;
//
//    public int getMode() {
//        return mode;
//    }
//
//    public void setMode(int mode) {
//        this.mode = mode;
//    }
//
//    public ITestCase getTestCase() {
//        return testCase;
//    }
//
//    public void setTestCase(ITestCase testcase) {
//        this.testCase = testcase;
//    }
//
//    public IDriverGenMessage compileAndLink(CommandConfig customCommandConfig) throws IOException, InterruptedException {
//        DriverGenMessage genMessage = new DriverGenMessage();
//
//        StringBuilder compileMsg = new StringBuilder();
//
//        Map<String, String> compilationCommands = customCommandConfig.getCompilationCommands();
//
//        String workspace = new AkaConfig().fromJson().getOpeningWorkspaceDirectory();
//        String directory = new File(workspace).getParentFile().getParentFile().getPath();
//        Compiler compiler = Environment.getInstance().getCompiler();
//
//        // Create an executable file
//        logger.debug("Compiling source code files");
//        for (String filePath : compilationCommands.keySet()) {
//            String compilationCommand = compilationCommands.get(filePath);
//
//            logger.debug("Compile test driver:\nExecuting " + compilationCommand);
//
//            String[] script = CompilerUtils.prepareForTerminal(compiler, compilationCommand);
//
//            String response = new Terminal(script, directory).get();
//
//            compileMsg.append(response).append("\n");
//        }
//
//        genMessage.setCompileMessage(compileMsg.toString());
//
//        logger.debug("Re compile all akaignore files");
//        ProjectClone.compileIgnoreFiles(directory, testCase);
//
//        String linkCommand = customCommandConfig.getLinkingCommand();
//        logger.debug("Linking test driver: " + linkCommand);
//
//        String[] linkScript = CompilerUtils.prepareForTerminal(compiler, linkCommand);
//        String linkResponse = new Terminal(linkScript, directory).get();
//        genMessage.setLinkMessage(linkResponse);
//
//        return genMessage;
//    }
//
//    public CommandConfig initializeCommandConfigToRunTestCase(ITestCase testCase) {
//        /*
//         * create the command file of the test case from the original command file
//         */
//        String rootCommandFile = new WorkspaceConfig().fromJson().getCommandFile();
//
//        CommandConfig commandConfig = testCase.generateCommands(rootCommandFile,
//                testCase.getExecutableFile());
//
//        commandConfig.exportToJson(new File(testCase.getCommandConfigFile()));
//
//        logger.debug("Create the command file for test case " + testCase.getName() + " at "
//                + testCase.getCommandConfigFile());
//
//        return commandConfig;
//    }
//
//    protected TestpathString_Marker readTestpathFromFile(ITestCase testCase) throws InterruptedException {
//        TestpathString_Marker encodedTestpath = new TestpathString_Marker();
//
//        int MAX_READ_FILE_NUMBER = 10;
//        int countReadFile = 0;
//
//        do {
//            logger.debug("Finish. We are getting a execution path from hard disk");
//            encodedTestpath.setEncodedTestpath(normalizeTestpathFromFile(
//                    Utils.readFileContent(testCase.getTestPathFile())));
//
//            if (encodedTestpath.getEncodedTestpath().length() == 0) {
//                //initialization = "";
//                Thread.sleep(10);
//            }
//
//            countReadFile++;
//        } while (encodedTestpath.getEncodedTestpath().length() == 0 && countReadFile <= MAX_READ_FILE_NUMBER);
//
//        return encodedTestpath;
//    }
//
//    protected String normalizeTestpathFromFile(String testpath) {
//        testpath = testpath.replace("\r\n", ITestpathInCFG.SEPARATE_BETWEEN_NODES)
//                .replace("\n\r", ITestpathInCFG.SEPARATE_BETWEEN_NODES)
//                .replace("\n", ITestpathInCFG.SEPARATE_BETWEEN_NODES)
//                .replace("\r", ITestpathInCFG.SEPARATE_BETWEEN_NODES);
//        if (testpath.equals(ITestpathInCFG.SEPARATE_BETWEEN_NODES))
//            testpath = "";
//        return testpath;
//    }
//
//    protected TestpathString_Marker shortenTestpath(TestpathString_Marker encodedTestpath) {
//        String[] executedStms = encodedTestpath.getEncodedTestpath().split(ITestpathInCFG.SEPARATE_BETWEEN_NODES);
//        if (executedStms.length > 0) {
//            int THRESHOLD = 200; // by default
//            if (executedStms.length >= THRESHOLD) {
//                logger.debug("Shorten test path to enhance code coverage computation speed: from "
//                        + executedStms.length + " to " + THRESHOLD);
//                StringBuilder tmp_shortenTp = new StringBuilder();
//
//                for (int i = 0; i < THRESHOLD - 1; i++) {
//                    tmp_shortenTp.append(executedStms[i]).append(ITestpathInCFG.SEPARATE_BETWEEN_NODES);
//                }
//
//                tmp_shortenTp.append(executedStms[THRESHOLD - 1]);
//                encodedTestpath.setEncodedTestpath(tmp_shortenTp.toString());
//            } else {
//                logger.debug("No need for shortening test path because it is not too long");
//            }
//        }
//        return encodedTestpath;
//    }
//
//    public static String refactorResultTrace(ITestCase testCase) {
//        final String END_TAG = ",";
//        String path = testCase.getExecutionResultTrace();
//
//        if (new File(path).exists()) {
//            String oldContent = Utils.readFileContent(path);
//            String newContent = oldContent.trim();
//            if (newContent.endsWith(END_TAG)) {
//                newContent = SpecialCharacter.OPEN_SQUARE_BRACE
//                        + newContent.substring(0, newContent.length() - END_TAG.length())
//                        + SpecialCharacter.CLOSE_SQUARE_BRACE;
//            }
//            Utils.writeContentToFile(newContent, path);
//            return newContent;
//        }
//
//        return SpecialCharacter.EMPTY;
//    }
//
//    protected boolean analyzeTestpathFile(TestCase testCase) throws Exception {
//        // Read hard disk until the test path is written into file completely
//        TestpathString_Marker encodedTestpath = readTestpathFromFile(testCase);
//
//        boolean success = true;
//
//        // shorten test path if it is too long
//        encodedTestpath = shortenTestpath(encodedTestpath);
//
//        if (encodedTestpath.getEncodedTestpath().length() > 0) {
//            // Only for logging
//            success = computeCoverage(encodedTestpath, testCase);
//
//            logger.debug("Retrieve the test path file "
//                    + testCase.getTestPathFile() + " successfully.");
//            logger.debug("Generate test paths for " + testCase.getName() + " sucessfully");
//
//        } else {
//            String msg = "The content of test path file is empty after execution";
//            logger.debug(msg);
//            if (/*getMode() == IN_EXECUTION_WITHOUT_GTEST_MODE
//                    || */getMode() == IN_EXECUTION_WITH_FRAMEWORK_TESTING_MODE) {
//                UIController.showErrorDialog(msg, "Test case execution", "Fail");
//                testCase.setStatus(TestCase.STATUS_FAILED);
//            }
//            success = false;
//            throw new Exception(msg);
//        }
//
//        return success;
//    }
//
//    protected boolean computeCoverage(TestpathString_Marker encodedTestpath, TestCase testCase) {
//        // compute coverage
//        logger.debug("Compute coverage given the test path");
//
//        // coverage computation
//        ISourcecodeFileNode srcNode = Utils.getSourcecodeFile(testCase.getFunctionNode());
//        String tpContent = Utils.readFileContent(testCase.getTestPathFile());
//
//        SourcecodeCoverageComputation computer = new SourcecodeCoverageComputation();
//        try {
//            computer.setTestpathContent(tpContent);
//            computer.setConsideredSourcecodeNode(srcNode);
//            computer.setCoverage(Environment.getInstance().getTypeofCoverage());
//            computer.compute();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        // highlighter
//        try {
//            SourcecodeHighlighterForCoverage highlighter = new SourcecodeHighlighterForCoverage();
//            highlighter.setSourcecode(srcNode.getAST().getRawSignature());
//            highlighter.setTestpathContent(tpContent);
//            highlighter.setSourcecodePath(srcNode.getAbsolutePath());
//            highlighter.setAllCFG(computer.getAllCFG());
//            highlighter.setTypeOfCoverage(computer.getCoverage());
//            highlighter.highlight();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        // log to details tab of the testcase
//        if (getMode() == IN_AUTOMATED_TESTDATA_GENERATION_MODE) {
//            // log to details tab of the testcase
//            StringBuilder tp = new StringBuilder();
//            List<String> stms = encodedTestpath.getStandardTestpathByProperty(
//                    FunctionInstrumentationForStatementvsBranch_Markerv2.START_OFFSET_IN_FUNCTION);
//
//            if (stms.size() > 0) {
//                for (String stm : stms)
//                    tp.append(stm).append("=>");
//                tp = new StringBuilder(tp.substring(0, tp.length() - 2)); //
//                logger.debug("Done. Offsets of execution test path [length=" + stms.size() + "] = " + tp);
//                TCExecutionDetailLogger.logDetailOfTestCase(testCase, "Test case path: " + tp.toString());
//            } else {
//                logger.debug("Done. Offsets of execution test path [length=0]");
//                TCExecutionDetailLogger.logDetailOfTestCase(testCase, "No path");
//            }
//        }
//
//        return tpContent.contains(TestPathUtils.END_TAG);
//    }
//
//    protected void showExecutionResultDialog(ITestCase testCase, String result) {
//        Alert.AlertType type;
//        String headerText;
//
//        if (result.contains(SourceConstant.FAILED_FLAG)) {
//            type = Alert.AlertType.ERROR;
//            headerText = "Fail to execute test case " + testCase.getName();
//        } else if (result.contains(SourceConstant.PASSED_FLAG)) {
//            type = Alert.AlertType.INFORMATION;
//            headerText = "Execute test case " + testCase.getName() + " successfully";
//        } else {
//            type = Alert.AlertType.WARNING;
//            headerText = "Fail to execute test case " + testCase.getName();
//
//            if (!result.endsWith(SpecialCharacter.LINE_BREAK))
//                result += SpecialCharacter.LINE_BREAK;
//
//            result += "Catch a runtime error when execute test case " + testCase.getName();
//        }
//
//        String content = result;
//
//        Platform.runLater(() -> UIController.showDetailDialog(type, "Execution Result", headerText, content));
//    }
//
//    @Override
//    public void initializeConfigurationOfTestcase(ITestCase testCase) {
//        /*
//         * Update test case
//         */
//        // test path
//        testCase.setTestPathFileDefault();
//        logger.debug("The test path file of test case " + testCase.getName() + ": " + testCase.getTestPathFile());
//
//        // executable file
//        testCase.setExecutableFileDefault();
//        logger.debug("Executable file of test case " + testCase.getName() + ": " + testCase.getExecutableFile());
//
//        // debug executable file
//        testCase.setDebugExecutableFileDefault();
//        logger.debug("Debug executable file of test case " + testCase.getName() + ": " + testCase.getDebugExecutableFile());
//
//        // command file
//        testCase.setCommandConfigFileDefault();
//        logger.debug("Command file of test case " + testCase.getName() + ": " + testCase.getCommandConfigFile());
//
//        // debug file
//        testCase.setCommandDebugFileDefault();
//        logger.debug("Debug command file of test case " + testCase.getName() + ": " + testCase.getCommandDebugFile());
//
//        // breakpoint
//        testCase.setBreakpointPathDefault();
//        logger.debug("Breakpoint file of test case " + testCase.getName() + ": " + testCase.getBreakpointPath());
//
//        // test case path
//        testCase.setTestPathFileDefault();
//        logger.debug("Path of the test case " + testCase.getName() + ": " + testCase.getTestPathFile());
//
//        // exec result path
//        if (Environment.getInstance().isC()) {
//            String path = new WorkspaceConfig().fromJson().getExecutionResultDirectory() + File.separator + testCase.getName() + "-Results.xml";
//            testCase.setExecutionResultFile(path);
//        } else {
//            testCase.setExecutionResultFileDefault();
//        }
//        logger.debug("Execute Result Path of the test case " + testCase.getName() + ": " + testCase.getExecutionResultTrace());
//
//        // source code file path
//        testCase.setSourcecodeFileDefault();
//        logger.debug("The source code file containing the test case " + testCase.getName() + ": " + testCase.getSourceCodeFile());
//
//        // execution date and time
//        testCase.setExecutionDateTime(LocalDateTime.now());
//
//        testCase.setExecutedTime(-1);
//
//        TestCaseManager.exportTestCaseToFile(testCase);
//    }
//
//    protected String runExecutableFile(CommandConfig commandConfig) throws IOException, InterruptedException {
//        String executableFilePath = commandConfig.getExecutablePath();
//        executableFilePath = PathUtils.toAbsolute(executableFilePath);
//
////        String workspace = new AkaConfig().fromJson().getOpeningWorkspaceDirectory();
//        String directory = new File(executableFilePath).getParent();
//
//        Terminal terminal;
//
////        if (mode == IN_EXECUTION_WITH_FRAMEWORK_TESTING_MODE) {
////            String[] executeCommand = new String[] {executableFilePath, "--gtest_output="
////                    + String.format("xml:%s", testCase.getExecutionResultFile())};
////
////            terminal = new Terminal(executeCommand, directory);
////
////        } else
//        terminal = new Terminal(executableFilePath, directory);
//
//        Process p = terminal.getProcess();
//        p.waitFor(10, TimeUnit.SECONDS); // give it a chance to stop
//
//        if (p.isAlive()) {
//            p.destroy(); // tell the process to stop
//            p.waitFor(10, TimeUnit.SECONDS); // give it a chance to stop
//            p.destroyForcibly(); // tell the OS to kill the process
//            p.waitFor();
//        }
//
//        testCase.setExecutedTime(terminal.getTime());
//
//        return terminal.get();
//    }
//
//    public TestDriverGeneration getTestDriverGeneration() {
//        return testDriverGen;
//    }
//
//    public void setTestDriverGeneration(TestDriverGeneration testDriverGeneration) {
//        this.testDriverGen = testDriverGeneration;
//    }
//}
