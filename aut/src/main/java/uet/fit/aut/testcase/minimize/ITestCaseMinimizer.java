package uet.fit.aut.testcase.minimize;

import uet.fit.aut.testcase.TestCase;

import java.util.List;

public interface ITestCaseMinimizer {

    List<TestCase> clean(String coverageType, List<TestCase> testCases, Scope scope);

    List<TestCase> minimize(String coverageType, List<TestCase> testCases, Scope scope) throws Exception;
}
