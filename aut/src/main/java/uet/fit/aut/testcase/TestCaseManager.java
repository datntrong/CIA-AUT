package uet.fit.aut.testcase;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.logger.ExternalLogger;
import uet.fit.aut.config.FolderConfig;
import uet.fit.aut.parser.obj.ICommonFunctionNode;

import java.io.File;
import java.util.Random;

public class TestCaseManager {

    private final static Logger logger = LoggerFactory.getLogger(TestCaseManager.class);

    private static TestCaseManager instance = null;

    public static TestCaseManager getInstance() {
        if (instance == null)
            instance = new TestCaseManager();
        return instance;
    }

    private ExternalLogger externalLogger;

//    private final Map<String, IDataTestItem> nameToBasicTestCaseMap = new HashMap<>();
//    private final Map<ICommonFunctionNode, Set<String>> functionToTestCasesMap = new HashMap<>();
//
//    private TestCaseManager() {
//        initializeMaps();
//    }
//
//    public void clearMaps() {
//        nameToBasicTestCaseMap.clear();
//        functionToTestCasesMap.clear();
//    }
//
//    public void initializeMaps() {
//        ProjectNode rootNode = Environment.getInstance().getProjectNode();
//        if (rootNode != null) {
//            // initialize nameToBasicTestCaseMap
//            List<IFunctionNode> nodes = Search.searchNodes(rootNode, new AbstractFunctionNodeCondition());
//            for (IFunctionNode functionNode : nodes) {
//                Set<String> testCaseNames = new HashSet<>();
//                functionToTestCasesMap.put(functionNode, testCaseNames);
//            }
//        } else {
//            logger.error("The project root node is null when initialize maps for TestcaseManager.");
//        }
//    }

    public TestPrototype createPrototype(String name, ICommonFunctionNode functionNode) {
        if (name == null || functionNode == null)
            return null;

        return new TestPrototype(functionNode, name);
    }

    public TestCase createTestCase(String name, ICommonFunctionNode functionNode) {
        if (name == null || functionNode == null)
            return null;

        TestCase testCase = new TestCase(functionNode, name);

        // init parameter expected outputs
        logger.debug("initParameterExpectedOuputs");
        ExternalLogger.log(externalLogger, "init parameter expected outputs");
        testCase.initParameterExpectedOutputs();

        logger.debug("initGlobalInputExpOutputMap");
        ExternalLogger.log(externalLogger, "init global input expected output map");
        testCase.initGlobalInputExpOutputMap();

//            exportBasicTestCaseToFile(testCase);
//            exportBreakpointsToFile(testCase);

        return testCase;
    }

    public TestCase createTestCase(ICommonFunctionNode functionNode, String nameTestcase) {
        TestCase testCase;
        String testCaseName;
        if (nameTestcase != null && !nameTestcase.isEmpty())
            testCaseName = AbstractTestCase.removeSpecialCharacter(nameTestcase);
        else {
            testCaseName = generateRandomName(functionNode.getSingleSimpleName());
        }
        testCase = createTestCase(testCaseName, functionNode);
        return testCase;
    }

    public TestPrototype createPrototype(ICommonFunctionNode functionNode, String name) {
        TestPrototype testCase;
        String testCaseName;
        if (name != null && !name.isEmpty())
            testCaseName = AbstractTestCase.removeSpecialCharacter(name);
        else {
            testCaseName = generateRandomName(functionNode.getSingleSimpleName());
        }
        testCase = createPrototype(testCaseName, functionNode);
        return testCase;
    }

    public TestCase createTestCase(@NotNull ICommonFunctionNode functionNode) {
        String prefix = functionNode.getSingleSimpleName();
        String testCaseName = generateRandomName(prefix + ITestCase.POSTFIX_TESTCASE_BY_USER);
        return createTestCase(testCaseName, functionNode);
    }

//    public ITestCase getTestCaseByName(String name) {
//        ITestCase testCase = getBasicTestCaseByName(name);
//
//        if (testCase == null)
//            logger.error(String.format("Test case %s not found.", name));
//
//        return testCase;
//    }
//
//    public TestCase getBasicTestCaseByName(String name) {
//        if (name == null)
//            return null;
//
//        // find in the map first
//        if (nameToBasicTestCaseMap.containsKey(name)) {
//            ITestItem itemInMap = nameToBasicTestCaseMap.get(name);
//            if (itemInMap instanceof TestCase) {
//                    return (TestCase) itemInMap;
//            }
//        }
//
//        return null;
//    }
//
//    public String getStatusTestCaseByName(String name) {
//        ITestCase testCase = getTestCaseByName(name);
//        if (testCase == null)
//            return ITestCase.STATUS_EMPTY;
//        else
//            return testCase.getStatus();
//    }
//
//    public void removeBasicTestCase(String name) {
//        TestCase testCase = getBasicTestCaseByName(name);
//        if (testCase != null) {
//            testCase.deleteOldData();
//            nameToBasicTestCaseMap.remove(name);
//
//            ICommonFunctionNode functionNode = testCase.getFunctionNode();
//            functionToTestCasesMap.get(functionNode).remove(name);
//
//        } else {
//            logger.error("Test case not found. Name: " + name);
//        }
//    }
//
//    /**
//     * This method to check if the testcase file (or compound testcase) that has the name
//     * exists or not
//     *
//     * @param name: name of the testcase (or compound testcase)
//     * @return true if find out a testcase or a compound testcase in directories
//     */
//    public boolean checkTestCaseExisted(String name) {
//        return nameToBasicTestCaseMap.containsKey(name);
//    }
//
//    public List<TestCase> getTestCasesByFunction(ICommonFunctionNode functionNode) {
//        List<TestCase> testCases = new ArrayList<>();
//        List<String> names = new ArrayList<>(functionToTestCasesMap.get(functionNode));
//        for (String name : names) {
//            TestCase tc = getBasicTestCaseByName(name);
//            if (tc != null)
//                testCases.add(tc);
//        }
//        return testCases;
//    }
//
//    public Map<ICommonFunctionNode, Set<String>> getFunctionToTestCasesMap() {
//        return functionToTestCasesMap;
//    }
//
//    public Map<String, IDataTestItem> getNameToBasicTestCaseMap() {
//        return nameToBasicTestCaseMap;
//    }

    public static synchronized String generateRandomName(String prefix){
        logger.debug("Generate name for test case");
        return prefix + new Random().nextInt(RANDOM_BOUND);
    }

    public synchronized static String generateContinuousNameOfTestcase(String testCaseNamePrefix){
        logger.debug("Generate name of test case name");
        return generateRandomName(testCaseNamePrefix);

//        for (int i = 0; i < RANDOM_BOUND; i++){
//            String candidateName = testCaseNamePrefix + ITestCase.AUT_SIGNAL + i;
//
//            // to create unique name temporarily
//            candidateName = AbstractTestCase.removeSpecialCharacter(candidateName); // to create unique name temporarily
//
////            if (generatedTestcaseNames.contains(candidateName))
////                continue;
//
//            String candidatePath;
//
//            candidatePath = FolderConfig.load().getWorkspace() + File.separator + candidateName
//                    + File.separator + candidateName + ".json";
//            if (new File(candidatePath).exists())
//                continue;
//            else {
////                generatedTestcaseNames.add(candidateName);
//                return candidateName;
//            }
//        }
//        return new Random().nextInt(9999999) + "";
    }

    private static final int RANDOM_BOUND = 9999999;

    public void setExternalLogger(ExternalLogger externalLogger) {
        this.externalLogger = externalLogger;
    }
}
