package uet.fit.aut.thread.task;

import uet.fit.aut.autogen.testdatagen.AbstractAutomatedTestdataGeneration;
import uet.fit.aut.autogen.testdatagen.BasisPathTestdataGeneration;
import uet.fit.aut.autogen.testdatagen.CFDSAutomatedTestdataGeneration;
import uet.fit.aut.autogen.testdatagen.UserCodeTestdataGeneration;
import uet.fit.aut.logger.IdMapping;
import uet.fit.aut.config.BuildConfig;
import uet.fit.aut.config.ProjectConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.logger.ExternalLogger;
import uet.fit.aut.parser.obj.ICommonFunctionNode;
import uet.fit.aut.testcase.TestCase;
import uet.fit.aut.testcase.TestPrototype;
import uet.fit.dto.test.AutoGenDTO;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * This thread takes responsibility for generating test data for a function
 */
public class GenerateTestdataTask extends AbstractAUTTask<List<TestCase>> {

    private final static Logger logger = LoggerFactory.getLogger(GenerateTestdataTask.class);

    // subprogram under test
    private ICommonFunctionNode function;

    // a prototype of template function
    private TestPrototype selectedPrototype;

    private final BuildConfig buildConfig;
    private final ProjectConfig projectConfig;
    private final String workspace;
    private final String environment;
    private final String coverageType;
    private HashMap<String, List<String>> userCode = new HashMap<>();

    // show report or not
    private boolean showReport;

    private ExternalLogger generalLogger, buildLogger;

    private final String taskId;

    private String strategy;

    public GenerateTestdataTask(BuildConfig buildConfig, ProjectConfig projectConfig,
            String workspace, String environment, String coverageType, HashMap<String, List<String>> userCode,
            String strategy) {
        this.buildConfig = buildConfig;
        this.projectConfig = projectConfig;
        this.workspace = workspace;
        this.environment = environment;
        this.coverageType = coverageType;
        this.taskId = UUID.randomUUID().toString();
        this.userCode = userCode;
        this.strategy = strategy;
    }

    @Override
	public List<TestCase> run() throws Exception {
        long startTime = System.currentTimeMillis();

        String msg = "Random test data generation";
        logger.debug(msg);
        ExternalLogger.log(generalLogger, msg);
        String taskTitle = "Generate tests for " + function.getSingleSimpleName();
        ExternalLogger.progress(generalLogger, taskId, taskTitle, 1, 5);
//        RandomAutomatedTestdataGeneration gen = new RandomAutomatedTestdataGeneration(function);
//        DFSAutomatedTestdataGeneration gen = new DFSAutomatedTestdataGeneration(function, "statement");
//        CFDSAutomatedTestdataGeneration

        AbstractAutomatedTestdataGeneration gen;
        switch (strategy) {
            case AutoGenDTO.CFDS:
                gen = new CFDSAutomatedTestdataGeneration(function, "STATEMENT");
                break;
            case AutoGenDTO.USER_CODE:
                if (userCode.isEmpty()) {
                    throw new Exception("User code is empty");
                }

                gen = new UserCodeTestdataGeneration(function, "STATEMENT");
                break;
            case AutoGenDTO.BASIS_PATH:
                gen = new BasisPathTestdataGeneration(function, "STATEMENT");
                break;
            default:
                throw new Exception("Not support strategy");
        }

        gen.setGeneralLogger(generalLogger);
        gen.setBuildLogger(buildLogger);
        gen.setEnvironment(environment);
        gen.setWorkspace(workspace);
        gen.setCoverageType(coverageType);
        gen.setShowReport(showReport);
        gen.setBuildConfig(buildConfig);
        gen.setProjectConfig(projectConfig);
        gen.setUserCode(userCode);
        gen.getAllPrototypes().add(selectedPrototype);

        long deltaTime = System.currentTimeMillis() - startTime;
        msg = "Generate test data automatically for function " + IdMapping.getInstance().getOrCreate(function.getSimpleName()) + " cost " + deltaTime + "ms";
        logger.debug(msg);
        msg = "Generate test data automatically for function " + function.getSimpleName() + " cost " + deltaTime + "ms";
        ExternalLogger.info(generalLogger, msg);
        ExternalLogger.progress(generalLogger, taskId, taskTitle, 3, 5);

        gen.generateTestdata(function);

        ExternalLogger.progress(generalLogger, taskId, taskTitle, 5, 5);

        logger.debug(msg);
        ExternalLogger.log(generalLogger, msg);

        return gen.getTestCases();

    }

    public ICommonFunctionNode getFunction() {
        return function;
    }

    public void setFunction(ICommonFunctionNode function) {
        this.function = function;
    }

    public void setGeneralLogger(ExternalLogger generalLogger) {
        this.generalLogger = generalLogger;
    }

    public void setBuildLogger(ExternalLogger buildLogger) {
        this.buildLogger = buildLogger;
    }
}