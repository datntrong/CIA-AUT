package uet.fit.aut.autogen.testdatagen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.autogen.cfg.ICFG;
import uet.fit.aut.coverage.SourcecodeCoverageComputation;
import uet.fit.aut.parser.obj.AbstractFunctionNode;
import uet.fit.aut.parser.obj.ICommonFunctionNode;
import uet.fit.aut.parser.obj.IFunctionNode;
import uet.fit.aut.parser.obj.ISourcecodeFileNode;
import uet.fit.aut.parser.obj.IVariableNode;
import uet.fit.aut.parser.obj.MacroFunctionNode;
import uet.fit.aut.testcase.ITestCase;
import uet.fit.aut.testcase.TestCase;
import uet.fit.aut.testcase.TestPrototype;
import uet.fit.aut.util.CFGUtils;
import uet.fit.aut.util.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
public class UserCodeTestdataGeneration extends SymbolicExecutionTestdataGeneration {
	private final static Logger logger = LoggerFactory.getLogger(UserCodeTestdataGeneration.class);

	public UserCodeTestdataGeneration(ICommonFunctionNode fn, String coverageType) {
		super(fn, coverageType);
	}

	protected void generateTestCase(List<TestCase> testCases, RandomAutomatedTestdataGeneration gen, HashMap<String,
			List<String>> userCode) {
		try {
			gen.setUserCode(userCode);
			TestCase testCase = gen.generateTestdata(0, fn,null, generatedTestcases);
			testCases.add(testCase);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void generateAllTestData(List<TestCase> testCases) {
		//config
		RandomAutomatedTestdataGeneration gen = new RandomAutomatedTestdataGeneration(fn);
		//gen.setLimitNumberOfIterations(getMaxIteration()+5);
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

		List<IVariableNode> arguments = fn.getArguments();
		HashMap<String, List<String>> userCodeForGentest = new HashMap<>();
		if (arguments.size() != 0) {
			generateTestDataForCombinationUserCode(testCases, 0, arguments, userCodeForGentest, gen);
		}
	}

	private void generateTestDataForCombinationUserCode(List<TestCase> testCases, int indexArgument,
			List<IVariableNode> arguments, HashMap<String, List<String>> userCodeForGentest,
			RandomAutomatedTestdataGeneration gen) {
		if (indexArgument == arguments.size()) {
			// gen test
			generateTestCase(testCases, gen, userCodeForGentest);

		} else {
			String nameArgument = arguments.get(indexArgument).getName();
			List<String> userCodeOfThisArgument = userCode.get(nameArgument);
			if (userCodeOfThisArgument == null) {
				generateTestDataForCombinationUserCode(testCases, indexArgument + 1, arguments,
						userCodeForGentest, gen);
			} else {
				for (int i = 0; i < userCodeOfThisArgument.size(); i++) {
					/* Update user code in hash */
					List<String> userCodeOfArgumentForGentest = new ArrayList<>();
					userCodeOfArgumentForGentest.add(userCodeOfThisArgument.get(i));
					userCodeForGentest.remove(nameArgument);
					userCodeForGentest.put(nameArgument, userCodeOfArgumentForGentest);
					generateTestDataForCombinationUserCode(testCases, indexArgument + 1, arguments,
							userCodeForGentest, gen);
				}
			}
		}
	}

	@Override
	protected void start(List<TestCase> testCases, ICommonFunctionNode fn, String coverageType,
			List<TestPrototype> allPrototypes, List<String> generatedTestcases, List<String> analyzedTestpathMd5,
			boolean showReport) throws Exception {
		// compute coverage & update CFG
		ICFG currentCFG = updateCFG(testCases, fn, coverageType);

		// Gen test
		generateAllTestData(testCases);
		execute(testCases);
		onGenerateSuccess(showReport);
		logger.debug("Done generation using user code");
	}

	protected String getTestCaseNamePrefix(ICommonFunctionNode fn)  {
		return fn.getSimpleName() + ITestCase.POSTFIX_TESTCASE_BY_DIRECTED_METHOD;
	}

	private int getMaxIteration() {
		List<IVariableNode> arguments = fn.getArguments();
		int maxIteration = 1;
		for (IVariableNode arg : arguments) {
			int size = userCode.get(arg.getName()).size();
			if (size > 1) {
				maxIteration *= size;
			}
		}
		return maxIteration;
	}

	protected ICFG updateCFG(List<TestCase> testCases, ICommonFunctionNode functionNode, String cov) {
		ICFG currentCFG = null;

		String allTestpaths = "";
		for (TestCase testCase : testCases)
			if (testCase.getTestPathFile() != null && new File(testCase.getTestPathFile()).exists())
				allTestpaths += Utils.readFileContent(testCase.getTestPathFile()) + "\n";

		if (allTestpaths.length() > 0) {
			/*
			 * Compute coverage of all test cases up to now
			 */
			logger.debug("Start computing total coverage");
			ISourcecodeFileNode sourcecodeNode = Utils.getSourcecodeFile(functionNode);
			SourcecodeCoverageComputation sourcecodeCoverageComputation = new SourcecodeCoverageComputation();
			sourcecodeCoverageComputation.setCoverage(cov);
			sourcecodeCoverageComputation.setConsideredSourcecodeNode(sourcecodeNode);
			sourcecodeCoverageComputation.setTestpathContent(allTestpaths);
			sourcecodeCoverageComputation.compute();
			logger.debug("Total coverage computation... DONE");

			/*
			 * Get cfg corresponding to the tested function
			 */
			List<ICFG> CFGs = sourcecodeCoverageComputation.getAllCFG();
			for (ICFG cfg : CFGs) {
				if (cfg.getFunctionNode().getAbsolutePath().equals(functionNode.getAbsolutePath())) {
					currentCFG = cfg;
					break;
				}
			}
		} else {
			/*
			 * There is no previous test case, just need to construct a cfg
			 */
			try {
				if (functionNode instanceof MacroFunctionNode) {
					IFunctionNode tmpFunctionNode = ((MacroFunctionNode) functionNode).getCorrespondingFunctionNode();
					currentCFG = CFGUtils.createCFG(tmpFunctionNode, cov);
					currentCFG.setFunctionNode(tmpFunctionNode);
				} else if (functionNode instanceof AbstractFunctionNode) {
					currentCFG = CFGUtils.createCFG((IFunctionNode) functionNode, cov);
					currentCFG.setFunctionNode((IFunctionNode) functionNode);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return currentCFG;
	}
}
