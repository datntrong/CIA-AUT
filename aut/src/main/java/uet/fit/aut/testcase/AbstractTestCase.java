package uet.fit.aut.testcase;

import uet.fit.aut.usercode.objects.TestCaseUserCode;
import uet.fit.aut.util.SpecialCharacter;
import uet.fit.aut.util.Utils;

import java.io.File;

public abstract class AbstractTestCase extends TestItem implements ITestCase {

    private String id;

    // some test cases need to be added some specified headers
    private String additionalHeaders = SpecialCharacter.EMPTY;

    // Not executed (by default)
    private String status = TestCase.STATUS_NA;

    // the file containing the test path after executing this test case
    private String testPathFile;
    private String executableFile;
    private String executionResultFile;

    // test case user code
    private TestCaseUserCode testCaseUserCode;

    protected AbstractTestCase() {
        super();
    }

    @Override
    public boolean isPrototypeTestcase() {
        return false;
    }

    // Is a part of test driver
    // The test driver of a test case has two files.
    // This file contains the main function to run a test case
    // This file is stored in {working-directory}/testdrivers)
    private String sourcecodeFile;

    public void deleteOldData() {
        if (getExecutionResultTrace() != null)
            Utils.deleteFileOrFolder(new File(getExecutionResultTrace()));

        if (getSourceCodeFile() != null)
            Utils.deleteFileOrFolder(new File(getSourceCodeFile()));

        if (getTestPathFile() != null)
            Utils.deleteFileOrFolder(new File(getTestPathFile()));
    }

    @Override
    public String getSourceCodeFile() {
        return sourcecodeFile;
    }

    @Override
    public void setSourceCodeFile(String sourcecodeFile) {
        this.sourcecodeFile = removeSysPathInName(sourcecodeFile);
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String getTestPathFile() {
        return testPathFile;
    }

    @Override
    public void setTestPathFile(String testPathFile) {
        this.testPathFile = removeSysPathInName(testPathFile);
    }

    @Override
    public String getExecutableFile() {
        return executableFile;
    }

    @Override
    public void setExecutableFile(String executableFile) {
        this.executableFile = removeSysPathInName(executableFile);
    }

    @Override
    public String getExecutionResultTrace() {
        return executionResultFile;
    }

    @Override
    public void setExecutionResultTrace(String path) {
        this.executionResultFile = removeSysPathInName(path);
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", getName(), status);
    }

    public String getAdditionalHeaders() {
        return additionalHeaders;
    }

    public void setAdditionalHeaders(String additionalHeaders) {
        this.additionalHeaders = additionalHeaders;
    }

    public void appendAdditionHeader(String includeStm) {
        if (additionalHeaders == null)
            additionalHeaders = includeStm;
        else if (!additionalHeaders.contains(includeStm))
            additionalHeaders += SpecialCharacter.LINE_BREAK + includeStm;
    }

    public void setTestCaseUserCode(TestCaseUserCode testCaseUserCode) {
        this.testCaseUserCode = testCaseUserCode;
    }

    public TestCaseUserCode getTestCaseUserCode() {
        if (testCaseUserCode == null) {
            testCaseUserCode = new TestCaseUserCode();
            testCaseUserCode.setSetUpContent("// set up\n");
            testCaseUserCode.setTearDownContent("// tear down\n");
        }
        return testCaseUserCode;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
