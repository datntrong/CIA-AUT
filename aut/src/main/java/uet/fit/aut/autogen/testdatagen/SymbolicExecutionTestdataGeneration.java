package uet.fit.aut.autogen.testdatagen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.autogen.cfg.object.*;
import uet.fit.aut.autogen.cfg.testpath.FullTestpath;
import uet.fit.aut.autogen.cfg.testpath.ITestpathInCFG;
import uet.fit.aut.autogen.testdatagen.se.*;
import uet.fit.aut.autogen.testdatagen.se.normalization.ConstraintNormalizer;
import uet.fit.aut.autogen.testdatagen.se.solver.RunZ3OnCMD;
import uet.fit.aut.autogen.testdatagen.se.solver.SmtLibGeneration;
import uet.fit.aut.autogen.testdatagen.se.solver.solutionparser.Z3SolutionParser;
import uet.fit.aut.config.FolderConfig;
import uet.fit.aut.instrument.ProjectClone;
import uet.fit.aut.logger.Locations;
import uet.fit.aut.testdata.ValueToTestcaseConverter_UnknownSize;
import uet.fit.aut.parser.finder.Level;
import uet.fit.aut.parser.finder.VariableSearchingSpace;
import uet.fit.aut.parser.obj.*;
import uet.fit.aut.search.Search;
import uet.fit.aut.search.condition.VariableNodeCondition;
import uet.fit.aut.testcase.TestCase;
import uet.fit.aut.testcase.TestCaseManager;
import uet.fit.aut.testcase.TestPrototype;
import uet.fit.aut.testdata.InputCellHandler;
import uet.fit.aut.testdata.gen.TreeExpander;
import uet.fit.aut.testdata.gen.module.InitialTreeGen;
import uet.fit.aut.testdata.gen.module.SimpleTreeDisplayer;
import uet.fit.aut.testdata.object.RootDataNode;
import uet.fit.aut.testdata.object.ValueDataNode;
import uet.fit.aut.util.IRegex;
import uet.fit.aut.util.ResourceFileUtils;
import uet.fit.aut.util.SpecialCharacter;
import uet.fit.aut.util.Utils;
import uet.fit.aut.util.VariableTypeUtils;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTNode;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class SymbolicExecutionTestdataGeneration extends AbstractAutomatedTestdataGeneration {

    private static final Logger logger = LoggerFactory.getLogger(ProjectClone.class);

    private static final String CONSTRAINT_DIR = "constraints";

    private final FolderConfig folderConfig = FolderConfig.load();
    protected static final String Z3_PATH = FolderConfig.load().getWorkspace() + "/z3_clone";
//    protected static final String Z3_PATH = "/home/chunglh/Dropbox/z3-4.8.9-x64-ubuntu/bin/z3";
//    protected final String Z3_PATH = "/Users/nguyentrongdat/Desktop/Run-CIA/z3";
    public SymbolicExecutionTestdataGeneration(ICommonFunctionNode fn, String coverageType) {
        super(fn);
        this.coverageType = coverageType;
    }

    public void generateTestdata(ICommonFunctionNode fn) throws Exception {
        logger.debug("Generating test data for function " + fn.getName());
        logger.debug("Automated test data generation strategy: Directed-Dijkstra");

        //TODO functionConfig
//        if (fn.getFunctionConfig() == null) {
//            FunctionConfig functionConfig = new WorkspaceConfig().fromJson().getDefaultFunctionConfig();
//            functionConfig.setFunctionNode(fn);
//            fn.setFunctionConfig(functionConfig);
//            functionConfig.createBoundOfArgument(functionConfig, fn);
//        }

//        final long MAX_ITERATION = fn.getFunctionConfig().getTheMaximumNumberOfIterations(); // may change to any value
//        logger.debug("Maximum number of iterations = " + MAX_ITERATION);

        // TODO template function
//        if (fn.isTemplate() || fn instanceof MacroFunctionNode || fn.hasVoidPointerArgument() || fn.hasFunctionPointerArgument()) {
//            if (this.allPrototypes == null || this.allPrototypes.size() == 0)
//                this.allPrototypes = getAllPrototypesOfTemplateFunction(fn);
//            if (this.allPrototypes.size() == 0)
//                return;
//        }

        testCases = new ArrayList<>();

        start(this.testCases, this.fn, this.coverageType, this.allPrototypes, this.generatedTestcases, this.analyzedTestpathMd5, showReport);
    }

    /**
     * Generate test data
     */
    protected abstract void start(List<TestCase> testCases, ICommonFunctionNode fn, String coverageType,
                         List<TestPrototype> allPrototypes,
                         List<String> generatedTestcases,
                         List<String> analyzedTestpathMd5,
                         boolean showReport) throws Exception;

    /**
     * Find a test case traversing the given test path
     *
     * @param testpath            a test path
     * @param fn                  the tested function
     * @param generatedTestcases  generated test cases
     * @param analyzedTestpathMd5 md5 of analyzed test paths (result of executing the generated test case)
     * @return state of solving process (FOUND_DUPLICATED_TESTCASE/ COULD_NOT_CONSTRUCT_TREE_FROM_TESTCASE/
     * COUND_NOT_EXECUTE_TESTCASE/ BE_ABLE_TO_EXECUTE_TESTCASE)
     */
    protected int solve(List<ICfgNode> testpath, ICommonFunctionNode fn, List<String> generatedTestcases,
                        List<String> analyzedTestpathMd5) {
        String tpStr = testpath.toString();
        String md5 = Utils.computeMd5(tpStr);
        if (analyzedTestpathMd5.contains(md5))
            return AUTOGEN_STATUS.FOUND_DUPLICATED_TESTPATH;

        analyzedTestpathMd5.add(md5);

        List<RandomValue> theNextTestdata = generateTheNextTestData(testpath, fn, generatedTestcases);

        if (theNextTestdata != null && theNextTestdata.size() > 0) {
            /*
             * Initialize a test case
             */
            String nameofTestcase = TestCaseManager.generateContinuousNameOfTestcase(getTestCaseNamePrefix(fn));
            TestCase testCase = new TestCaseManager().createTestCase(nameofTestcase, fn);

            /*
             * Execute a test case
             */
            if (testCase != null) {
//                TestcaseExecution executor = new TestcaseExecution();
//                executor.setFunction(fn);
//                executor.setMode(TestcaseExecution.IN_AUTOMATED_TESTDATA_GENERATION_MODE);
                int executionStatus = iterateDirectly(testCase, fn, theNextTestdata);

                if (executionStatus == AUTOGEN_STATUS.EXECUTION.BE_ABLE_TO_EXECUTE_TESTCASE) {
                    testCases.add(testCase);
                }

                return executionStatus;
            } else
                return AUTOGEN_STATUS.OTHER_ERRORS;

        } else {
            logger.debug("There is no next test data");
            return AUTOGEN_STATUS.SOLVING_STATUS.FOUND_DUPLICATED_TESTCASE;
        }
    }

    protected abstract String getTestCaseNamePrefix(ICommonFunctionNode fn);

    /**
     * @param testCase
     * @param fn
     * @param theNextTestdata
     * @return COULD_NOT_CONSTRUCT_TREE_FROM_TESTCASE/ COUND_NOT_EXECUTE_TESTCASE/ BE_ABLE_TO_EXECUTE_TESTCASE
     */
    protected int iterateDirectly(TestCase testCase, ICommonFunctionNode fn,
                                  List<RandomValue> theNextTestdata) {
        RootDataNode root = testCase.getRootDataNode();

        try {
            logger.debug("recursiveExpandUutBranch");
            recursiveExpandUutBranch(root.getRoot(), theNextTestdata, testCase);
        } catch (Exception e) {
            e.printStackTrace();
            return AUTOGEN_STATUS.EXECUTION.COULD_NOT_CONSTRUCT_TREE_FROM_TESTCASE;
        }

        try {
            logger.debug(new SimpleTreeDisplayer().toString(root));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return executeTestCase(testCase, "");
    }

    protected int executeTestCase(TestCase testCase, String additionalHeaders) {
        try {
            testCase.setStatus(TestCase.STATUS_EXECUTING);

            testCase.setAdditionalHeaders(additionalHeaders);

            String coverage = coverageType;

            // Execute random values
            execute(Collections.singletonList(testCase));

            //TODO return testcases to save in TestCaseServices
//            String path = FolderConfig.load().getEnvironment() + File.separator + environment + File.separator
//                    + TEST_CASE_DIR + File.separator + newId + ".json";
//            testCase.setPath(FolderConfig.load().getEnvironment() );
//            TestCaseManager.exportBasicTestCaseToFile(testCase);
//            logger.debug("Save the testcase " + testCase.getName() + " to file " + testCase.getPath());

            if (testCase.getStatus().equals(TestCase.STATUS_SUCCESS)
                    || testCase.getStatus().equals(TestCase.STATUS_RUNTIME_ERR)) {
                // export highlighted source code and coverage to file
                //TODO save on RAM
//                CoverageManager.exportCoveragesOfTestCaseToFile(testCase, coverage);

                // read coverage information from file to display on GUI
                List<TestCase> testcases = new ArrayList<>();
                testcases.add(testCase);

                return AUTOGEN_STATUS.EXECUTION.BE_ABLE_TO_EXECUTE_TESTCASE;
            } else {
                logger.debug("Do not add test case " + testCase.getName() + " because we can not execute it");
                return AUTOGEN_STATUS.EXECUTION.COUND_NOT_EXECUTE_TESTCASE;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return AUTOGEN_STATUS.EXECUTION.COUND_NOT_EXECUTE_TESTCASE;
        }
    }

    protected List<RandomValue> generateTheNextTestData(List<ICfgNode> testpath, ICommonFunctionNode functionNode,
                                                        List<String> generatedTestcases) {
        List<RandomValue> theNextTestdata = new ArrayList<>();
        Parameters paramaters = new Parameters();
        paramaters.addAll(functionNode.getArgumentsAndGlobalVariables());

        try {
            ITestpathInCFG testpathInCFG = new FullTestpath();
            testpathInCFG.getAllCfgNodes().addAll(testpath);
            ISymbolicExecution se = new SymbolicExecution(testpathInCFG, paramaters, functionNode);
            logger.debug("Constraints: " + se.getConstraints());
            logger.debug("new variables: " + se.getNewVariables().toString());
            logger.debug("table mapping: " + se.getTableMapping().toString());

            // add new variables to parameters
//            for (NewVariableInSe newVar : se.getNewVariables()) {
//                boolean isExist = paramaters.stream().anyMatch(v -> v.getName().equals(newVar.getOriginalName()));
//                if (!isExist) {
//                    IVariableNode var = findCorrespondingVar(newVar, paramaters);
//                    if (var == null) {
//                        var = new VariableNode();
//                        var.setRawType(VariableTypeUtils.BASIC.STDINT.UINT8__T);
//                        var.setCoreType(VariableTypeUtils.BASIC.STDINT.UINT8__T);
//                        var.setName(newVar.getOriginalName());
//                    }
//                    paramaters.add(var);
//                }
//            }

            // add new variables to parameters
            List<NewVariableInSe> newVariables = new ArrayList<>(se.getNewVariables());
            newVariables.sort(new Comparator<NewVariableInSe>() {
                @Override
                public int compare(NewVariableInSe o1, NewVariableInSe o2) {
                    return o1.getOriginalName().compareTo(o2.getOriginalName());
                }
            });
            for (NewVariableInSe newVar : newVariables) {
                boolean isExist = paramaters.stream().anyMatch(v -> v.getName().equals(newVar.getOriginalName()));
                if (!isExist) {
                    IVariableNode var = findCorrespondingVar(newVar, paramaters);
                    if (var == null) {
                        var = new VariableNode();
                        var.setRawType(VariableTypeUtils.BASIC.CHARACTER.CHAR);
                        var.setCoreType(VariableTypeUtils.BASIC.CHARACTER.CHAR);
                        var.setName(newVar.getOriginalName());
                    }
                    paramaters.add(var);
                }
            }

            // generate smt-lib2
            SmtLibGeneration smt = new SmtLibGeneration(paramaters, (List<PathConstraint>) se.getNormalizedPathConstraints(), functionNode, se.getNewVariables());
            smt.generate();
            logger.debug("SMT-LIB file:\n" + smt.getSmtLibContent());

            String constraintFile = generateRandomConstraintPath();
            logger.debug("constraintFile: " + constraintFile);
            Utils.writeContentToFile(smt.getSmtLibContent(), constraintFile);

            // solve
            logger.debug("Calling solver z3");
            if (!new File(Z3_PATH).exists()) {
                ResourceFileUtils.copyFile(Z3_PATH, Locations.Z3_PATH);
                Utils.chmod777(new File(Z3_PATH));
            }

            RunZ3OnCMD z3Runner = new RunZ3OnCMD(Z3_PATH, constraintFile);
            z3Runner.execute();

            logger.debug("Original solution:\n" + z3Runner.getSolution());
            String staticSolution = new Z3SolutionParser().getSolution(z3Runner.getSolution());

            onZ3Solution(se, paramaters, z3Runner.getSolution());

            if (!staticSolution.trim().isEmpty()) {
                String md5 = Utils.computeMd5(staticSolution);
                if (!generatedTestcases.contains(md5)) {
                    generatedTestcases.add(md5);
                    logger.debug("the next test data = " + staticSolution);
                    logger.debug("Convert to standard format");
                    ValueToTestcaseConverter_UnknownSize converter = new ValueToTestcaseConverter_UnknownSize(staticSolution);
                    List<RandomValue> randomValues = converter.convert();
                    logger.debug(randomValues.toString());
                    //xu ly truong hop so sanh hai con tro (can toi uu lai)
                    //TODO: doi getConstraints() thanh normolize....
                    for (PathConstraint constraint : (PathConstraints) se.getConstraints()) {
                        //normalize constraint
                        ConstraintNormalizer norm = new ConstraintNormalizer();
                        norm.setOriginalSourcecode(constraint.getConstraint());
                        norm.normalize();
                        //convert to AST
                        IASTNode astConstraint = Utils.convertToIAST(norm.getNormalizedSourcecode());
                        ASTVisitor visitor = new ASTVisitor() {
                            @Override
                            public int visit(IASTExpression expression) {
                                if (expression instanceof IASTBinaryExpression) {
                                    if (((IASTBinaryExpression) expression).getOperator() == IASTBinaryExpression.op_equals) {
                                        String leftOperand = ((IASTBinaryExpression) expression).getOperand1().getRawSignature();
                                        String rightOperand = ((IASTBinaryExpression) expression).getOperand2().getRawSignature();
                                        //swap leftOp with rightOp if neccessary
                                        if (rightOperand.contains(leftOperand)) {
                                            String tmp = rightOperand;
                                            rightOperand = leftOperand;
                                            leftOperand = tmp;
                                        }
                                        if (!rightOperand.equals("NULL")) {
                                            RandomValueForAssignment newValue = new RandomValueForAssignment(leftOperand, rightOperand);
                                            randomValues.add(newValue);
                                        }
                                    }
                                }
                                return super.visit(expression);
                            }
                        };
                        visitor.shouldVisitExpressions = true;
                        astConstraint.accept(visitor);
                    }

                    theNextTestdata = randomValues;
                    theNextTestdata = addTrackerValuesForFunctionCall(((SymbolicExecution) se).getAutoStubGen(), theNextTestdata);
                } else {
                    logger.debug("The next test data exists! Ignoring...");
                    theNextTestdata = new ArrayList<>();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return theNextTestdata;
    }

    protected String generateRandomConstraintPath() {
//        String constraintFile = environment + File.separator + CONSTRAINT_DIR + File.separator
//                + new RandomDataGenerator().nextInt(0, 99999) + ".smt2";
//        String constraintFile = folderConfig.getEnvironment() + File.separator + environment + File.separator
//                + CONSTRAINT_DIR + File.separator + new RandomDataGenerator().nextInt(0, 99999) + ".smt2";
        String constraintFile = environment + File.separator+ CONSTRAINT_DIR + File.separator
                + new RandomDataGenerator().nextInt(0, 99999) + ".smt2";

        return constraintFile;
    }

    private List<RandomValue> addTrackerValuesForFunctionCall(AutoStubGeneration autoStubGen, List<RandomValue> theNextTestdata) {
        HashMap<String, Integer> functionToOrdinalNumber = autoStubGen.getFunctionToOrdinalNumber();
        for (Map.Entry<String, Integer> entry : functionToOrdinalNumber.entrySet()){
            String functionName = entry.getKey() + autoStubGen.AUTO_STUB_POSTFIX;
            String value = Integer.toString(entry.getValue());
            RandomValue randomValue = new RandomValue(functionName, value);
            theNextTestdata.add(randomValue);
        }
        return theNextTestdata;
    }

    private IVariableNode findCorrespondingVar(NewVariableInSe newVar, List<IVariableNode> variableNodes) {
        String name = newVar.getOriginalName().trim();
        boolean isChildOfArray = name.endsWith("]");
        String encodedName = name.replaceAll(IRegex.ARRAY_INDEX, SpecialCharacter.EMPTY);
        String[] nameItems = encodedName.split("\\.");
        IVariableNode lastNode = variableNodes.stream()
                .filter(v -> v.getName().equals(nameItems[0]))
                .findFirst()
                .orElse(null);
        if (lastNode != null) {
            for (int i = 1; i < nameItems.length; i++) {
                List<Level> space = new VariableSearchingSpace(lastNode).getSpaces();
                List<INode> nodes = Search.searchInSpace(space, new VariableNodeCondition(), nameItems[i]);
                if (!nodes.isEmpty()) {
                    lastNode = (IVariableNode) nodes.get(0);
                } else {
                    return null;
                }
            }

            if (isChildOfArray) {
                try {
                    ValueDataNode dataNode = new InitialTreeGen().genInitialTree((VariableNode) lastNode, new RootDataNode());
                    int dim = Utils.getIndexOfArray(name).size();
                    ValueDataNode childNode;
                    if (dim > 1) {
                        childNode = (ValueDataNode) new TreeExpander().generateArrayItem(name, dataNode);
                    } else {
                        new InputCellHandler().commitEdit(dataNode, "1");
                        childNode = (ValueDataNode) dataNode.getChildren().get(0);
                    }
                    IVariableNode v = childNode.getCorrespondingVar();
                    v.setName(newVar.getOriginalName());
                    return v;
                } catch (Exception e) {
                    return null;
                }
            } else {
                IVariableNode v = lastNode.clone();
                v.setName(newVar.getOriginalName());
                return v;
            }
        }

        return null;
    }
    protected void onZ3Solution(ISymbolicExecution se, Parameters parameters, String solution) {

    }
}