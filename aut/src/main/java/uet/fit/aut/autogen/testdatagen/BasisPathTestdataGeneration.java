package uet.fit.aut.autogen.testdatagen;


import uet.fit.aut.autogen.cfg.ICFG;
import uet.fit.aut.autogen.cfg.object.ICfgNode;
import uet.fit.aut.config.FunctionConfig;
import uet.fit.aut.coverage.basicpath.BasicPath;
import uet.fit.aut.coverage.basicpath.BasicPathsObtain;
import uet.fit.aut.env.Environment;
import uet.fit.aut.logger.AUTLogger;
import uet.fit.aut.logger.Locations;
import uet.fit.aut.parser.obj.AbstractFunctionNode;
import uet.fit.aut.parser.obj.ICommonFunctionNode;
import uet.fit.aut.parser.obj.IFunctionNode;
import uet.fit.aut.parser.obj.MacroFunctionNode;
import uet.fit.aut.testcase.ITestCase;
import uet.fit.aut.testcase.TestCase;
import uet.fit.aut.testcase.TestCaseManager;
import uet.fit.aut.testcase.TestPrototype;
import uet.fit.aut.thread.task.ExecuteTestTask;
import uet.fit.aut.util.CFGUtils;
import uet.fit.aut.util.ResourceFileUtils;
import uet.fit.aut.util.Utils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class BasisPathTestdataGeneration extends SymbolicExecutionTestdataGeneration {

    private final static AUTLogger logger = AUTLogger.get(BasisPathTestdataGeneration.class);

    public BasisPathTestdataGeneration(ICommonFunctionNode fn, String coverageType) {
        super(fn, coverageType);
        setExecute(false);
    }

    @Override
    protected void start(List<TestCase> testCases, ICommonFunctionNode fn, String coverageType,
            List<TestPrototype> allPrototypes, List<String> generatedTestcases, List<String> analyzedTestpathMd5,
            boolean showReport) {
        ICFG currentCFG = null;
        try {
            if (fn instanceof MacroFunctionNode) {
                IFunctionNode tmpFunctionNode = ((MacroFunctionNode) fn).getCorrespondingFunctionNode();
                currentCFG = CFGUtils.createCFG(tmpFunctionNode, coverageType);
                currentCFG.setFunctionNode(tmpFunctionNode);
            } else if (fn instanceof AbstractFunctionNode) {
                currentCFG = CFGUtils.createCFG((IFunctionNode) fn, coverageType);
                currentCFG.setFunctionNode((IFunctionNode) fn);
            }
        } catch (Exception e) {
            logger.error("Cant generate CFG for " + fn.getName());
            e.printStackTrace();
        }

        if (fn.getFunctionConfig() == null) {
            FunctionConfig functionConfig = Environment.getInstance().getDefaultFunctionConfig();
            functionConfig.setFunctionNode(fn);
            fn.setFunctionConfig(functionConfig);
            functionConfig.createBoundOfArgument(functionConfig, fn);
        }

        try {
            if (!new File(Z3_PATH).exists()) {
                ResourceFileUtils.copyFile(Z3_PATH, Locations.Z3_PATH);
                Utils.chmod777(new File(Z3_PATH));
            }
        }  catch (Exception e) {
            e.printStackTrace();
        }

        if (currentCFG != null) {
            logger.debug("Generate CFG for " + fn.getName() + " successfully");
            Set<BasicPath> basicPaths = new BasicPathsObtain(currentCFG).set();
            logger.debug("Number of basis paths: " + basicPaths.size());

            boolean isGenerateDefault = false;
            for (BasicPath basicPath : basicPaths) {
                List<ICfgNode> normalizedPath = normalizePath(basicPath);
                logger.debug("Consider path: " + normalizedPath);
                int code = solve(normalizedPath, fn, generatedTestcases, analyzedTestpathMd5);
                if (code != AUTOGEN_STATUS.EXECUTION.BE_ABLE_TO_EXECUTE_TESTCASE) {
                    logger.error("Cant generate test case using " + basicPath);
                    if (!isGenerateDefault) {
                        TestCase emptyTestCase = TestCaseManager.getInstance().createTestCase(fn);
                        testCases.add(emptyTestCase);
                        isGenerateDefault = true;
                    } else {
                        try {
                            RandomAutomatedTestdataGeneration gen = new RandomAutomatedTestdataGeneration(fn);
                            gen.setLimitNumberOfIterations(1);
                            gen.setShouldRunParallel(false);
                            gen.setShowReport(false);
                            gen.getAllPrototypes().addAll(allPrototypes);
                            gen.setProjectConfig(projectConfig);
                            gen.setGeneralLogger(generalLogger);
                            gen.setBuildLogger(buildLogger);
                            gen.setEnvironment(environment);
                            gen.setWorkspace(workspace);
                            gen.setCoverageType(coverageType);
                            gen.setBuildConfig(buildConfig);
                            gen.setUserCode(userCode);
                            gen.setExecute(false);
                            gen.generateTestdata(fn);
                            testCases.addAll(gen.getTestCases());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            onGenerateSuccess(showReport);
        }
    }

    @Override
    protected String getTestCaseNamePrefix(ICommonFunctionNode fn) {
        return fn.getSimpleName() + ITestCase.POSTFIX_TESTCASE_BY_BASIS_METHOD;
    }
}
