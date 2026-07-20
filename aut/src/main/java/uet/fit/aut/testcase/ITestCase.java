package uet.fit.aut.testcase;

import uet.fit.aut.parser.obj.ICommonFunctionNode;
import uet.fit.aut.usercode.objects.TestCaseUserCode;

public interface ITestCase extends ITestItem {

    String POSTFIX_TESTCASE_BY_USER = ".manual";
    String POSTFIX_TESTCASE_BY_RANDOM = ".random";
    String POSTFIX_TESTCASE_BY_BOUNDARY = ".boundary";
    String POSTFIX_TESTCASE_BY_DATA_TYPE_BOUNDARY = ".mmm";
    String POSTFIX_TESTCASE_BY_DIRECTED_METHOD = ".directed";
    String POSTFIX_TESTCASE_BY_NORMAL_BOUNDARY = ".normbound";
    String POSTFIX_TESTCASE_BY_BVA = ".bva";
    String POSTFIX_TESTCASE_BY_ROBUSTNESS = ".robustness";
    String POSTFIX_TESTCASE_BY_BASIS_METHOD = ".basis";

    String COMPOUND_SIGNAL = "COMPOUND";
    String AUT_SIGNAL = ".";

    String STATUS_NA = "N/A";
    String STATUS_EXECUTING = "executing";
    String STATUS_SUCCESS = "success";
    String STATUS_FAILED = "failed";
    String STATUS_RUNTIME_ERR = "runtime error";
    String STATUS_EMPTY = "no status";

//    void setExecutionDateTime(LocalDateTime executionDateTime);
//
//    LocalDateTime getExecutionDateTime();
//
//    String getExecutionDate();
//
//    String getExecutionTime();

    String getStatus();

    void setStatus(String status);

//    AssertionResult getExecutionResult();

//    void setExecutionResult(AssertionResult result);

//    void appendExecutionResult(AssertionResult result);

    String getSourceCodeFile();

    void setSourceCodeFile(String sourcecodeFile);

    String getTestPathFile();

    void setTestPathFile(String testPathFile);

    String getExecutableFile();

    void setExecutableFile(String executableFile);

    void deleteOldData(); // delete all files related to the current test case

    String getAdditionalHeaders();

    void setAdditionalHeaders(String additionalHeaders);

    void setTestCaseUserCode(TestCaseUserCode testCaseUserCode);

    TestCaseUserCode getTestCaseUserCode();

//    String getExecuteLog();
//
//    void appendExecuteLog(String line);
//
//    double getExecutedTime();
//
//    void setExecutedTime(double executedTime);

    String getExecutionResultTrace();

    void setExecutionResultTrace(String path);

    void setId(String id);

    String getId();

    ICommonFunctionNode getFunctionNode();

//    String STATEMENT_COVERAGE_FILE_EXTENSION = ".stm.cov";
//    String BRANCH_COVERAGE_FILE_EXTENSION = ".branch.cov";
//    String BASIS_PATH_COVERAGE_FILE_EXTENSION = ".basispath.cov";
//    String MCDC_COVERAGE_FILE_EXTENSION = ".mcdc.cov";
//
//    String STATEMENT_PROGRESS_FILE_EXTENSION = ".stm.pro";
//    String BRANCH_PROGRESS_FILE_EXTENSION = ".branch.pro";
//    String BASIS_PATH_PROGRESS_FILE_EXTENSION = ".basispath.pro";
//    String MCDC_PROGRESS_FILE_EXTENSION = ".mcdc.pro";
}
