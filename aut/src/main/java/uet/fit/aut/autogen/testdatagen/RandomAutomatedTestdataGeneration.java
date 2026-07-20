package uet.fit.aut.autogen.testdatagen;

import uet.fit.aut.logger.IdMapping;
import uet.fit.aut.logger.TimeTracker;
import uet.fit.aut.config.FunctionConfig;
import uet.fit.aut.coverage.CoverageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.env.Environment;
import uet.fit.aut.logger.ExternalLogger;
import uet.fit.aut.parser.obj.ConstructorNode;
import uet.fit.aut.parser.obj.ICommonFunctionNode;
import uet.fit.aut.parser.obj.MacroFunctionNode;
import uet.fit.aut.testcase.TestCase;
import uet.fit.aut.testcase.TestPrototype;
import uet.fit.aut.testcase.minimize.GreedyMinimizer;
import uet.fit.aut.testcase.minimize.ITestCaseMinimizer;
import uet.fit.aut.testcase.minimize.Scope;
import uet.fit.aut.testdata.object.RootDataNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RandomAutomatedTestdataGeneration extends AbstractAutomatedTestdataGeneration {

    private final static Logger logger = LoggerFactory.getLogger(RandomAutomatedTestdataGeneration.class);

    /**
     * To avoid too many iterations
     */
    private static long limitNumberOfIterations = 100000;

    public RandomAutomatedTestdataGeneration(ICommonFunctionNode fn) {
        super(fn);
    }

    /**
     * Start generating test cases for a function
     *
     * @param fn a function
     * @throws Exception
     */
    public void generateTestdata(ICommonFunctionNode fn) throws Exception {
        logger.debug("Generating test data for function " + IdMapping.getInstance().getOrCreate(fn.getName()));
        logger.debug("Automated test data generation strategy: random");
        ExternalLogger.log(generalLogger, "Automated test data generation strategy: random");

        /**
         * Initialize function config if the function does not have any.
         */
        if (fn.getFunctionConfig() == null) {
            FunctionConfig functionConfig = Environment.getInstance().getDefaultFunctionConfig();
            functionConfig.setFunctionNode(fn);
            fn.setFunctionConfig(functionConfig);
            functionConfig.createBoundOfArgument(functionConfig, fn);
        }

        if (fn.getFunctionConfig().getFunctionNode() == null)
            fn.getFunctionConfig().setFunctionNode(fn);

        long MAX_ITERATON = fn.getFunctionConfig().getTheMaximumNumberOfIterations();

        // to avoid too many iterations
        MAX_ITERATON = Math.min(MAX_ITERATON, limitNumberOfIterations);

        logger.debug("Maximum number of iterations = " + MAX_ITERATON);

        // clear cache before generate testcase
//        TestCasesTreeItem treeItem = CacheHelper.getFunctionToTreeItemMap().get(fn);
//        if (CacheHelper.getTreeItemToListTestCasesMap().get(treeItem) != null)
//            CacheHelper.getTreeItemToListTestCasesMap().get(treeItem).clear();

        if (fn.isTemplate() || fn instanceof MacroFunctionNode || fn.hasVoidPointerArgument() || fn.hasFunctionPointerArgument()) {
            // Get all prototypes if the tested function has
//            if (this.allPrototypes == null || this.allPrototypes.size() == 0)
//                this.allPrototypes = getAllPrototypesOfTemplateFunction(fn);
            if (this.allPrototypes.size() == 0)
                return;

            for (TestPrototype prototype : allPrototypes)
                start(MAX_ITERATON, prototype);
        } else {
            // The tested function is a normal function.
            start(MAX_ITERATON, null);
        }

        // view coverage of generated set of test cases
//        if (!shouldRunParallel) {
//            onGenerateSuccess(showReport);
//            LoadingPopupController.getInstance().close();
//        }
    }

    /**
     *
     * @param maxIteration the maximum number of iterations
     * @param selectedPrototype the selected prototype if it exists
     */
    protected void start(final long maxIteration, TestPrototype selectedPrototype) throws Exception {
        TestCase initTestCase = generateIteration(0, maxIteration, selectedPrototype);
        execute(Collections.singletonList(initTestCase));
        testCases.add(initTestCase);

        float coverage = CoverageManager.getCoverageAtFunctionLevel(coverageType, initTestCase);
        if (coverage == 1)
            return;

        List<TestCase> extendTestCases = new ArrayList<>();
        for (int iteration = 1; iteration < maxIteration; iteration++) {
            TestCase testCase = generateIteration(iteration, maxIteration, selectedPrototype);
            if (testCase != null) {
                extendTestCases.add(testCase);
            }
        }

        execute(extendTestCases);

        testCases.addAll(extendTestCases);

        logger.debug("Done random test data generation");
        ExternalLogger.info(generalLogger, "Done random test data generation");

        ITestCaseMinimizer minimizer = new GreedyMinimizer();
        testCases = minimizer.clean(coverageType, testCases, Scope.SOURCE);
    }

    private TestCase generateIteration(int iteration, final long maxIteration, TestPrototype selectedPrototype) throws Exception {
        logger.debug("Iteration " + (iteration + 1) + "/" + maxIteration);
        ExternalLogger.log(generalLogger, "Iteration " + (iteration + 1) + "/" + maxIteration);
        long startTime = System.currentTimeMillis();
        TestCase testCase = generateTestdata(iteration, fn, selectedPrototype, generatedTestcases);
        TimeTracker.add("Generate test #" + iteration, System.currentTimeMillis() - startTime);
        return testCase;
    }

    protected TestCase generateTestdata(int iteration, ICommonFunctionNode fn, TestPrototype selectedPrototype,
                                        List<String> generatedTestcases) throws Exception {
        TestCase testCase = createTestcase(selectedPrototype, iteration, fn);
        if (testCase == null)
            return null;
        RootDataNode root = testCase.getRootDataNode();
        List<String> additionalHeaders = new ArrayList<>();
        String newTestCaseInStr = "";

        // generate random value for parameters
        ICommonFunctionNode sut = testCase.getFunctionNode();
        if (!(sut instanceof ConstructorNode)) {
            logger.debug("Generate random value for new test case");
            List<RandomValue> randomValuesForArguments = generateRandomValueForArguments(root, sut, additionalHeaders, selectedPrototype, testCase);
            if (randomValuesForArguments != null)
                newTestCaseInStr += randomValuesForArguments.toString();
            logger.debug("randomValuesForArguments: " + randomValuesForArguments);
        }

        // generate random value for instance
        if (!sut.isStatic()) {
            List<RandomValue> randomValuesForInstance = generateRandomValuesForInstance(root, sut, testCase);
            if (randomValuesForInstance != null)
                newTestCaseInStr += randomValuesForInstance.toString();
            logger.debug("randomValuesForInstance = " + randomValuesForInstance);
        }

        // generate random value for global variables
        List<RandomValue> randomValuesForGlobalVariables = generateRandomValuesForGlobal(root, sut, testCase);
        if (randomValuesForGlobalVariables.size() != 0)
            newTestCaseInStr += randomValuesForGlobalVariables.toString();
        logger.debug("randomValuesForGlobalVariables = " + randomValuesForGlobalVariables);

        if (generatedTestcases.contains(newTestCaseInStr)) {
            logger.debug("Duplicate test cases. Ignore.");
            testCase.deleteOldData();
            return null;
        }

        List<RandomValue> randomValuesForStubs = generateRandomValuesForStub(root, sut, testCase);
        if (randomValuesForStubs != null)
            newTestCaseInStr += randomValuesForStubs.toString();
        logger.debug("randomValuesForStubs = " + randomValuesForStubs);

        generatedTestcases.add(newTestCaseInStr);

        String additionalHeadersAll = "";
        for (String item : additionalHeaders)
            additionalHeadersAll += item;
        testCase.setAdditionalHeaders(additionalHeadersAll);

        return testCase;
    }

    public void setLimitNumberOfIterations(long limitNumberOfIterations) {
        this.limitNumberOfIterations = limitNumberOfIterations;
    }
}
