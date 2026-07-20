package uet.fit.aut.util;

import uet.fit.aut.autogen.cfg.CFGGenerationforBranchvsStatementvsBasispathCoverage;
import uet.fit.aut.autogen.cfg.CFGGenerationforSubConditionCoverage;
import uet.fit.aut.autogen.cfg.ICFG;
import uet.fit.aut.autogen.maker.TestpathString_Marker;
import uet.fit.aut.autogen.testdatagen.coverage.CFGUpdater;
import uet.fit.aut.coverage.highlight.AbstractHighlighterForSourcecodeLevel;
import uet.fit.aut.env.CoverageType;
import uet.fit.aut.instrument.IFunctionInstrumentationGeneration;
import uet.fit.aut.parser.obj.FunctionNode;
import uet.fit.aut.parser.obj.ICommonFunctionNode;
import uet.fit.aut.parser.obj.IFunctionNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.search.Search;
import uet.fit.aut.search.condition.AbstractFunctionNodeCondition;
import uet.fit.aut.testcase.TestCase;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static uet.fit.aut.coverage.AbstractCoverageComputation.getValue;
import static uet.fit.aut.coverage.CoverageManager.removeRedundantLineBreak;

public class CFGUtils {

	public static List<ICFG> getAndMarkAllCFG(String coverageType, TestCase testCase) {
		File testPath = new File(testCase.getTestPathFile());

		ICommonFunctionNode sut = testCase.getRootDataNode().getFunctionNode();
		INode sourceNode = Utils.getSourcecodeFile(sut);

		List<ICFG> cfgList = new ArrayList<>();

		Search.searchNodes(sourceNode, new AbstractFunctionNodeCondition())
				.stream()
				.map(f -> (IFunctionNode) f)
				.forEach(function -> {
					ICFG cfg = getAndMarkCFG(coverageType, function, testPath);
					if (cfg != null)
						cfgList.add(cfg);
				});

		return cfgList;
	}

	public static ICFG getAndMarkCFG(String coverageType, TestCase testCase) {
		ICommonFunctionNode sut = testCase.getRootDataNode().getFunctionNode();

		if (!(sut instanceof IFunctionNode))
			return null;

		IFunctionNode function = (IFunctionNode) sut;

		File testPath = new File(testCase.getTestPathFile());

		if (testPath.exists()) {
			return getAndMarkCFG(coverageType, function, testPath);
		}

		return null;
	}

	private static ICFG getAndMarkCFG(String coverageType, IFunctionNode function, File testPath) {
		String content = Utils.readFileContent(testPath);
		String[] lines = removeRedundantLineBreak(content).split("\\R");

		List<String> filterLines = new ArrayList<>();

		for (String testpath : lines) {
			String functionAddress = getValue(testpath, IFunctionInstrumentationGeneration.FUNCTION_ADDRESS);

			if (functionAddress != null && !functionAddress.isEmpty()) {

				if (!PathUtils.equals(functionAddress, function.getAbsolutePath()))
					continue;

				switch (coverageType) {
					case CoverageType.BRANCH:
					case CoverageType.BASIS_PATH:
					case CoverageType.STATEMENT: {
						if (AbstractHighlighterForSourcecodeLevel.isSubCondition(testpath))
							// ignore the test path which goes through subcondition
							continue;
						else
							break;
					}

					case CoverageType.MCDC: {
						//TODO: offset
						if (AbstractHighlighterForSourcecodeLevel.isFullCondition(testpath))
							// ignore the test path which goes through full condition
							continue;
						else
							break;
					}
				}

				filterLines.add(testpath);
			}
		}

		ICFG cfg = createCFG(function, coverageType);

		// Update the cfg
		TestpathString_Marker marker = new TestpathString_Marker();
		marker.setEncodedTestpath(filterLines.toArray(new String[0]));

		CFGUpdater updater = new CFGUpdater(marker, cfg);
		updater.updateVisitedNodes();

		return cfg;
	}

//    public static ICFG createExpandCFG(IFunctionNode functionNode, String coverageType) throws Exception {
//        ICFG cfg = createCFG(functionNode, coverageType);
//
//        for (ICfgNode cfgNode : cfg.getAllNodes()) {
//            if (cfgNode instanceof NormalCfgNode) {
//                IASTNode astNode = ((NormalCfgNode) cfgNode).getAst();
//
//                CalledCFGGeneration calledCFGGeneration = new CalledCFGGeneration(functionNode, coverageType);
//
//                astNode.accept(calledCFGGeneration);
//
//                ICFG calledCFG = calledCFGGeneration.getCalledCFG();
//            }
//        }
//
//        return cfg;
//    }

	/**
	 * Create CFG of a function.
	 *
	 * This function may call to a macro function or not.
	 *
	 * In case of a call to macro functions, CDT might parse the macro call inside the function, which
	 * might lead to the incorrect CFG.
	 * For example:
	 * #define MACRO_CALL(a) if (a>0) return 1; else return 0;
	 * int test(){return MACRO_CALL(a);}
	 * Consider test(), we need to get CFG of test() only, without considering the body of MACRO_CALL(a).
	 *
	 *
	 *
	 * Therefore, to disable the problem of macro expansion in CFG generation of the function,
	 * we need to disable macro.
	 */
	public static ICFG createCFG(IFunctionNode fn, String coverageType) {
		if (fn == null)
			return null;

		/*
		 * Find existing cfg of the function node
		 */
		ICFG cfg = null;
//		switch (coverageType) {
//			case CoverageType.STATEMENT:
//			case CoverageType.BRANCH:
//			case CoverageType.BASIS_PATH:{
//				cfg = Environment.getInstance().getCfgsForBranchAndStatement().get(fn.getAbsolutePath());
//				break;
//			}
//			case CoverageType.MCDC: {
//				cfg = Environment.getInstance().getCfgsForMcdc().get(fn.getAbsolutePath());
//				break;
//			}
//		}
		// STEP 1: Create a function with disable macro flag
		FunctionNode tmpFunction = new FunctionNode();
		tmpFunction.setAST(fn.getAST());
		tmpFunction.setAbsolutePath(fn.getAbsolutePath());
		tmpFunction.setParent(fn.getParent());

		// STEP 2: generate CFG of the alternative function
		switch (coverageType) {
			case CoverageType.STATEMENT_AND_BRANCH:
			case CoverageType.STATEMENT:
			case CoverageType.BRANCH:
			case CoverageType.BASIS_PATH: {
				cfg = new CFGGenerationforBranchvsStatementvsBasispathCoverage(tmpFunction).generateCFG();
				break;
			}

			case CoverageType.STATEMENT_AND_MCDC:
			case CoverageType.MCDC: {
				cfg = new CFGGenerationforSubConditionCoverage(tmpFunction).generateCFG();
				break;
			}
		}

		if (cfg != null) {
			cfg.setFunctionNode(fn);
			cfg.resetVisitedStateOfNodes();
			cfg.setIdforAllNodes();
		}
		return cfg;
	}
}

