package uet.fit.aut.testcase.minimize;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.parser.obj.ICommonFunctionNode;
import uet.fit.aut.testcase.TestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class TestCaseMinimizer implements ITestCaseMinimizer {

    public static final Logger logger = LoggerFactory.getLogger(TestCaseMinimizer.class);

    public List<TestCase> clean(String coverageType, List<TestCase> testCases, Scope scope) {
        List<TestCase> optimizes = new ArrayList<>();

        final int originSize = testCases.size();

        if (originSize != 1) {
            logger.debug("Grouping test cases by subprogram");
            Map<ICommonFunctionNode, List<TestCase>> group = groupTestCaseByFunction(testCases);

            for (Map.Entry<ICommonFunctionNode, List<TestCase>> entry : group.entrySet()) {
                try {
                    List<TestCase> list = entry.getValue();
                    if (list.size() > 1) {
                        logger.debug("Optimizing test set " + list);

                        long before = System.currentTimeMillis();

                        optimizes.addAll(minimize(coverageType, list, scope));

                        // keep at least 1 test case
                        if (optimizes.isEmpty())
                            optimizes.add(testCases.get(0));

                        logger.debug("Test set " + list + " -> " + optimizes);

                        List<TestCase> unnecessary = list.stream()
                                .filter(tc -> !optimizes.contains(tc))
                                .collect(Collectors.toList());

                        long after = System.currentTimeMillis();
                        long executedTime = after - before;
                        logger.debug("Optimizing time: " + executedTime + "ms");

                        deleteTestCase(unnecessary);
                    } else {
                        optimizes.addAll(list);
                    }
                } catch (Exception ex) {
                    logger.error("Can't optimize test set", ex);
                }
            }

            logger.debug("Optimize test cases done: " + originSize + " -> " + optimizes.size());
        } else {
            optimizes.addAll(testCases);
        }

        return optimizes;
    }

    private void deleteTestCase(List<TestCase> unnecessary) {
        for (TestCase testCase : unnecessary) {
            String name = testCase.getName();
//            TestCaseManager.getInstance().removeBasicTestCase(name);
        }
    }

    // Grouping test case by subprogram under test
    protected static Map<ICommonFunctionNode, List<TestCase>> groupTestCaseByFunction(List<TestCase> testCases) {
        Map<ICommonFunctionNode, List<TestCase>> map = new HashMap<>();

        for (TestCase testCase : testCases) {
            if (testCase != null) {
                ICommonFunctionNode sut = testCase.getFunctionNode();

                List<TestCase> list = map.get(sut);
                if (list == null)
                    list = new ArrayList<>();

                if (!list.contains(testCase))
                    list.add(testCase);

                map.put(sut, list);
            }
        }

        return map;
    }
}
