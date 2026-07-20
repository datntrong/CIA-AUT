package uet.fit.aut.coverage;

import uet.fit.aut.coverage.highlight.SourcecodeHighlighterForCoverage;
import uet.fit.aut.parser.obj.ISourcecodeFileNode;
import uet.fit.aut.testcase.TestCase;
import uet.fit.aut.util.Utils;

import java.io.File;
import java.util.List;

public class CoverageManager {

	public static final String EMPTY = "";
	public static final float ZERO_COVERAGE = 0;

	public static String highlightCoverage(String coverageType, TestCase... testCases) {
		List<TestCase> testList = List.of(testCases);
		CoverageData coverageData = getCoverageAtFileLevel(testList, coverageType);
		if (coverageData != null)
			return coverageData.getContent();
		return null;
	}

	/**
	 *
	 * @param testCases test cases of a function
	 * @param typeOfCoverage
	 * @return
	 */
	public static CoverageData getCoverageAtFunctionLevel(List<TestCase> testCases, String typeOfCoverage) {
		if (testCases == null || testCases.size() == 0)
			return null;

		// get all test paths
		String allTestpaths = "";
		for (TestCase testCase : testCases)
			if (testCase != null && testCase.getTestPathFile() != null && new File(testCase.getTestPathFile()).exists())
				allTestpaths += Utils.readFileContent(testCase.getTestPathFile()) + "\n";

		// coverage
		if (allTestpaths.length() > 0 && testCases.get(0) != null) {
			CoverageData coverageData = new CoverageData();

			ISourcecodeFileNode sourcecodeNode = Utils.getSourcecodeFile(testCases.get(0).getFunctionNode());
			FunctionCoverageComputation covComputation = new FunctionCoverageComputation();
			covComputation.setFunctionNode(testCases.get(0).getFunctionNode());
			covComputation.setCoverage(typeOfCoverage);
			covComputation.setConsideredSourcecodeNode(sourcecodeNode);
			covComputation.setTestpathContent(allTestpaths);
			covComputation.compute();
			coverageData.setProgress(covComputation.getNumberOfVisitedInstructions() * 1.0f / covComputation.getNumberOfInstructions());
			coverageData.setTotal(covComputation.getNumberOfInstructions());
			coverageData.setVisited(covComputation.getNumberOfVisitedInstructions());


			// highlight after coverage computation
			SourcecodeHighlighterForCoverage sourcecodeHighlighter = new SourcecodeHighlighterForCoverage();
			sourcecodeHighlighter.setTypeOfCoverage(typeOfCoverage);
			sourcecodeHighlighter.setAllCFG(covComputation.getAllCFG());
			sourcecodeHighlighter.setSourcecode(Utils.readFileContent(sourcecodeNode.getAbsolutePath()));
			sourcecodeHighlighter.setSourcecodePath(sourcecodeNode.getAbsolutePath());
			sourcecodeHighlighter.setTestpathContent(allTestpaths);
			sourcecodeHighlighter.highlight();
			String fullHighlight = sourcecodeHighlighter.getFullHighlightedSourcecode();
			coverageData.setContent(fullHighlight);

			return coverageData;
		} else if (testCases.get(0) != null && testCases.get(0).getFunctionNode() != null){
			// we have compilable test cases, but we can not execute them successfully
			CoverageData coverageData = new CoverageData();

			ISourcecodeFileNode sourcecodeNode = Utils.getSourcecodeFile(testCases.get(0).getFunctionNode());
			FunctionCoverageComputation covComputation = new FunctionCoverageComputation();
			covComputation.setCoverage(typeOfCoverage);
			covComputation.setConsideredSourcecodeNode(sourcecodeNode);
			covComputation.setTestpathContent(allTestpaths);
			covComputation.setFunctionNode(testCases.get(0).getFunctionNode());
			covComputation.compute();

			coverageData.setProgress(Float.NaN);
			coverageData.setTotal(covComputation.getNumberOfInstructions());
			coverageData.setVisited(0);
			return coverageData;
		} else
			return null;
	}

	public static float getCoverageAtFunctionLevel(String coverageType, TestCase... testCases) {
		List<TestCase> testList = List.of(testCases);
		CoverageData coverageData = getCoverageAtFunctionLevel(testList, coverageType);
		if (coverageData == null)
			return -1;
		else
			return coverageData.getProgress();
	}

	/**
	 * Compute coverage of multiple test cases
	 * @param testCases test cases of a function
	 * @param typeOfCoverage
	 * @return
	 */
	public static CoverageData getCoverageAtFileLevel(List<TestCase> testCases, String typeOfCoverage) {
		if (testCases == null || testCases.size() == 0)
			return null;

		// get all test paths
		String allTestpaths = "";
		for (TestCase testCase : testCases)
			if (testCase != null && testCase.getTestPathFile() != null && new File(testCase.getTestPathFile()).exists())
				allTestpaths += Utils.readFileContent(testCase.getTestPathFile()) + "\n";

		// coverage
		if (allTestpaths.length() > 0 && testCases.get(0) != null) {
			CoverageData coverageData = new CoverageData();

			ISourcecodeFileNode sourcecodeNode = Utils.getSourcecodeFile(testCases.get(0).getFunctionNode());
			SourcecodeCoverageComputation sourcecodeCoverageComputation = new SourcecodeCoverageComputation();
			sourcecodeCoverageComputation.setCoverage(typeOfCoverage);
			sourcecodeCoverageComputation.setConsideredSourcecodeNode(sourcecodeNode);
			sourcecodeCoverageComputation.setTestpathContent(allTestpaths);
			sourcecodeCoverageComputation.compute();
			coverageData.setProgress(sourcecodeCoverageComputation.getNumberOfVisitedInstructions() * 1.0f / sourcecodeCoverageComputation.getNumberOfInstructions());
			coverageData.setTotal(sourcecodeCoverageComputation.getNumberOfInstructions());
			coverageData.setVisited(sourcecodeCoverageComputation.getNumberOfVisitedInstructions());

			// highlight after coverage computation
			SourcecodeHighlighterForCoverage sourcecodeHighlighter = new SourcecodeHighlighterForCoverage();
			sourcecodeHighlighter.setTypeOfCoverage(typeOfCoverage);
			sourcecodeHighlighter.setAllCFG(sourcecodeCoverageComputation.getAllCFG());
			sourcecodeHighlighter.setSourcecode(Utils.readFileContent(sourcecodeNode.getAbsolutePath()));
			sourcecodeHighlighter.setSourcecodePath(sourcecodeNode.getAbsolutePath());
			sourcecodeHighlighter.setTestpathContent(allTestpaths);
			sourcecodeHighlighter.highlight();
			String fullHighlight = sourcecodeHighlighter.getFullHighlightedSourcecode();
			coverageData.setContent(fullHighlight);

			return coverageData;

		} else if (testCases.get(0) != null && testCases.get(0).getFunctionNode() != null){
			// we have compilable test cases, but we can not execute them successfully
			CoverageData coverageData = new CoverageData();

			ISourcecodeFileNode sourcecodeNode = Utils.getSourcecodeFile(testCases.get(0).getFunctionNode());
			SourcecodeCoverageComputation sourcecodeCoverageComputation = new SourcecodeCoverageComputation();
			sourcecodeCoverageComputation.setCoverage(typeOfCoverage);
			sourcecodeCoverageComputation.setConsideredSourcecodeNode(sourcecodeNode);
			sourcecodeCoverageComputation.setTestpathContent(allTestpaths);
			sourcecodeCoverageComputation.compute();

			coverageData.setProgress(Float.NaN);
			coverageData.setTotal(sourcecodeCoverageComputation.getNumberOfInstructions());
			coverageData.setVisited(0);
			return coverageData;
		}

		return null;
	}

	public static String removeRedundantLineBreak(String content) {
		content = content.replace("\r", "\n");
		content = content.replace("\n\n", "\n");
		return content;
	}

}
