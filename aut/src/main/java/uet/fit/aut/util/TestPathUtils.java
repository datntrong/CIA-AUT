package uet.fit.aut.util;

import uet.fit.aut.autogen.cfg.ICFG;
import uet.fit.aut.coverage.function_call.ConstructorCall;
import uet.fit.aut.coverage.function_call.FunctionCall;
import uet.fit.aut.env.CoverageType;
import uet.fit.aut.parser.obj.ICommonFunctionNode;
import uet.fit.aut.testcase.ITestCase;
import uet.fit.aut.testcase.TestCase;
import uet.fit.aut.testcase.minimize.Scope;
import uet.fit.aut.testcase.minimize.TestCaseMinimizer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestPathUtils {

    public static final String CALLING_TAG = "Calling: ";
    public static final String SKIP_TAG = "SKIP ";
    public static final String BEGIN_TAG = "BEGIN OF ";
    public static final String END_TAG = "END OF ";
    public static final String RETURN_TAG = "Return from: ";
    private static final String PRE_CALLING_TAG = "<<PRE-CALLING>>";
    private static final String DELIMITER = "|";

    public static boolean isCompleted(String testPath, String testName) {
        testPath = testPath.trim();
        return testPath.contains(END_TAG + testName);
    }

    public static List<FunctionCall> traceFunctionCall(String[] lines) {
        List<FunctionCall> calledFunctions = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            if (lines[i].startsWith(CALLING_TAG)) {
                String rawPath = lines[i].substring(CALLING_TAG.length());
                rawPath = PathUtils.normalize(rawPath);

                FunctionCall call = createFunctionCall(rawPath);

                if (i > 0 && lines[i - 1].startsWith(PRE_CALLING_TAG))
                    call.setCategory(FunctionCall.Position.FIRST);
                else
                    call.setCategory(FunctionCall.Position.MIDDLE);

                call.setIndex(calledFunctions.size());

                calledFunctions.add(call);

                int iterator = (int) calledFunctions.stream()
                        .filter(c -> PathUtils.equals(c.getAbsolutePath(), call.getAbsolutePath()))
                        .count();

                call.setIterator(iterator);

            } else if (lines[i].startsWith(RETURN_TAG)) {
                String rawPath = lines[i].substring(RETURN_TAG.length());
                rawPath = PathUtils.normalize(rawPath);

                FunctionCall call = createFunctionCall(rawPath);

                call.setCategory(FunctionCall.Position.LAST);
                call.setIndex(calledFunctions.size());
                calledFunctions.add(call);
            }
        }

        return calledFunctions;
    }

    private static FunctionCall createFunctionCall(String rawPath) {
        FunctionCall call;

        // constructor call
        if (rawPath.contains(DELIMITER)) {
            String[] paths = rawPath.split("\\Q" + DELIMITER + "\\E");
            call = new ConstructorCall();
            call.setAbsolutePath(paths[0]);
            ((ConstructorCall) call).setParameterPath(paths[1]);
        }
        // normal function call
        else {
            call = new FunctionCall();
            call.setAbsolutePath(rawPath);
        }

        return call;
    }

    public static List<Object> getVisited(TestCase testCase, Scope scope, String coverageType) {
        List<Object> visited = new ArrayList<>();

        File testPath = new File(testCase.getTestPathFile());

        if (testPath.exists()) {
            List<ICFG> cfgList = null;
            if (scope == Scope.FUNCTION) {
                ICFG cfg = CFGUtils.getAndMarkCFG(coverageType, testCase);
                if (cfg != null)
                    cfgList = Collections.singletonList(cfg);
            } else
                cfgList = CFGUtils.getAndMarkAllCFG(coverageType, testCase);

            if (cfgList != null) {
                for (ICFG cfg : cfgList) {
                    switch (coverageType) {
                        case CoverageType.BASIS_PATH:
                            visited.addAll(cfg.getVisitedBasisPaths());
                            break;

                        case CoverageType.STATEMENT:
                            visited.addAll(cfg.getVisitedStatements());
                            break;

                        case CoverageType.BRANCH:
                        case CoverageType.MCDC:
                            visited.addAll(cfg.getVisitedBranches());
                            break;

                        case CoverageType.STATEMENT_AND_BRANCH:
                        case CoverageType.STATEMENT_AND_MCDC:
                            visited.addAll(cfg.getVisitedStatements());
                            visited.addAll(cfg.getVisitedBranches());
                            break;
                    }
                }
            }
        }

        return visited;
    }

    private static List<Object> getVisitedOfCurrentType(TestCase testCase, Scope scope, String coverageType) {
        return getVisited(testCase, scope, coverageType);
    }

    public static int compare(TestCase testCase, List<TestCase> other, Scope scope, String coverageType) {
        ICommonFunctionNode sut = testCase.getFunctionNode();

        if (!isExecutable(testCase))
            return HAVENT_EXEC;

        List<Object> visited1 = getVisitedOfCurrentType(testCase, scope, coverageType);
        List<Object> visited2 = new ArrayList<>();

        for (TestCase tc : other) {
            TestCaseMinimizer.logger.debug("Comparing test case " + testCase.getName() + " with " + tc.getName());
            if (!tc.getFunctionNode().equals(sut))
                return ERR_COMPARE;

            if (isExecutable(tc)) {
                List<Object> visited = getVisitedOfCurrentType(tc, scope, coverageType);
                for (Object node : visited) {
                    if (!visited2.contains(node))
                        visited2.add(node);
                }
            }
        }

        if (visited2.isEmpty())
            return HAVENT_EXEC * -1;

        return compare(visited1, visited2);
    }

    public static int compare(List<Object> visited1, List<Object> visited2) {
        int result;

        if (visited1 == null || visited2 == null)
            return ERR_COMPARE;
        else if (visited1.size() >= visited2.size())
            result = 1;
        else
            result = -1;

        List<Object> commonPart = new ArrayList<>();

        for (Object node1 : visited1) {
            for (Object node2 : visited2) {
                if (node1.equals(node2)) {
                    commonPart.add(node1);
                }
            }
        }

        if (commonPart.isEmpty()) {
            // do nothing
        } else if (commonPart.size() == visited1.size() && commonPart.size() == visited2.size())
            result *= EQUAL;
        else if (commonPart.size() == visited1.size() || commonPart.size() == visited2.size())
            result *= CONTAIN_GT;
        else
            result *= COMMON_GT;

        return result;

    }

    private static boolean isExecutable(ITestCase testCase) {
        return testCase.getStatus().equals(ITestCase.STATUS_RUNTIME_ERR)
                || testCase.getStatus().equals(ITestCase.STATUS_SUCCESS);
    }

    public static final int EQUAL = 0;
    public static final int SEPARATED_GT = 1;
    public static final int COMMON_GT = 2;
    public static final int CONTAIN_GT = 3;
    public static final int ERR_COMPARE = 4;
    public static final int HAVENT_EXEC = 5;
}
