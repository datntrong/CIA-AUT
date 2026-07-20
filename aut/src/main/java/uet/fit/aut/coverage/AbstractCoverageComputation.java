package uet.fit.aut.coverage;

import uet.fit.aut.logger.IdMapping;
import uet.fit.aut.autogen.cfg.ICFG;
import uet.fit.aut.autogen.maker.TestpathString_Marker;
import uet.fit.aut.autogen.testdatagen.coverage.CFGUpdater;
import uet.fit.aut.coverage.highlight.AbstractHighlighterForSourcecodeLevel;
import uet.fit.aut.env.CoverageType;
import uet.fit.aut.instrument.IFunctionInstrumentationGeneration;
import uet.fit.aut.logger.AUTLogger;
import uet.fit.aut.parser.obj.AbstractFunctionNode;
import uet.fit.aut.parser.obj.IFunctionNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.search.Search;
import uet.fit.aut.search.condition.AbstractFunctionNodeCondition;
import uet.fit.aut.util.CFGUtils;
import uet.fit.aut.util.TestPathUtils;
import uet.fit.aut.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractCoverageComputation implements ICoverageComputation {

	private final static AUTLogger logger = AUTLogger.get(AbstractCoverageComputation.class);

	protected String testpathContent; // may visit may source code files
	protected INode consideredSourcecodeNode; // the source code file which we need to compute coverage at file level
	protected String coverage;
	protected int numberOfVisitedInstructions; // depend on coverage, instruction is statement, condition, or sub-condition
	protected int numberOfInstructions;
	protected List<ICFG> allCFG = new ArrayList<>();

	protected abstract Map<String, TestpathsOfAFunction> removeRedundantTestpath(Map<String, TestpathsOfAFunction> affectedFunctions);

	protected int getNumberOfInstructions(INode consideredSourcecodeNode, String coverage) {
		int nInstructions = 0;
		switch (coverage) {
			case CoverageType.STATEMENT: {
				nInstructions = getNumberofStatements(consideredSourcecodeNode);
				break;
			}
			case CoverageType.BRANCH: {
				nInstructions = getNumberofBranches(consideredSourcecodeNode);
				break;
			}

			case CoverageType.MCDC: {
				nInstructions = getNumberofMcdcs(consideredSourcecodeNode);
				break;
			}

			case CoverageType.BASIS_PATH: {
				nInstructions = getNumberOfBasisPath(consideredSourcecodeNode);
				break;
			}
		}
		return nInstructions;
	}

	/**
	 * @return the number of visited instructions (>=0), return -1 if there is error happening
	 */
	protected int getNumberOfVisitedInstructions(Map<String, TestpathsOfAFunction> affectedFunctions,
			String coverage, INode consideredSourcecodeNode,
			List<ICFG> allCFG) {
		final int ERROR = -1;
		int nVisitedInstructions = 0;
		for (String functionPath : affectedFunctions.keySet())
			// only consider functions in a specified source code file
			if (functionPath.contains(consideredSourcecodeNode.getAbsolutePath())) {
				logger.debug("Analyzing " + IdMapping.getInstance().getOrCreate(functionPath));
				// Find the function node
				TestpathsOfAFunction testpathsOfAFunction = affectedFunctions.get(functionPath);

				List<INode> functionNodes = Search.searchNodes(consideredSourcecodeNode, new AbstractFunctionNodeCondition(), functionPath);

				if (functionNodes.size() != 1)
					return ERROR;

				// generate cfg of the function
				INode functionNode = functionNodes.get(0);

				ICFG cfg = null;

				if (functionNode instanceof AbstractFunctionNode) {
					cfg = CFGUtils.createCFG((IFunctionNode) functionNode, coverage);
					allCFG.add(cfg);
					cfg.setFunctionNode((IFunctionNode) functionNode);
				}

				if (cfg == null)
					return ERROR;

				// compute coverage of a cfg
				TestpathString_Marker testpath = new TestpathString_Marker();

				if (testpathsOfAFunction != null && testpathsOfAFunction.getTestpathsInArray() != null)
					testpath.setEncodedTestpath(testpathsOfAFunction.getTestpathsInArray());
				else
					testpath.setEncodedTestpath(new String[]{});

				CFGUpdater cfgUpdater = new CFGUpdater(testpath, cfg);
				cfgUpdater.updateVisitedNodes();
				switch (coverage) {
					case CoverageType.STATEMENT: {
						nVisitedInstructions += cfg.getVisitedStatements().size();
						break;
					}
					case CoverageType.MCDC:
					case CoverageType.BRANCH: {
						nVisitedInstructions += cfg.getVisitedBranches().size();
						break;
					}

					case CoverageType.BASIS_PATH: {
						nVisitedInstructions += cfg.getVisitedBasisPaths().size();
						break;
					}
				}
				logger.debug("Num of visited instructions = " + nVisitedInstructions);
			}
		return nVisitedInstructions;
	}

	/**
	 * @return a hash map, where key is the path of function, value is a list.
	 * Each item in list is corresponding to a test path.
	 */
	protected Map<String, TestpathsOfAFunction> categoryTestpathByFunctionPath(String[] testpaths, String coverage) {
		Map<String, TestpathsOfAFunction> tps = new HashMap<>();

        TestCaseBegin testCaseBegin = null;

        for (String testpath : testpaths) {
            String functionAddress = getValue(testpath, IFunctionInstrumentationGeneration.FUNCTION_ADDRESS);

            if (testpath.startsWith(TestPathUtils.BEGIN_TAG)) {
                testCaseBegin = new TestCaseBegin();
                testCaseBegin.testpath = testpath;
            }

            if (functionAddress != null && functionAddress.length() > 0) {

				switch (coverage) {
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
						if (AbstractHighlighterForSourcecodeLevel.isFullCondition(testpath))
							// ignore the test path which goes through full condition
							continue;
						else
							break;
					}
				}

                if (!tps.containsKey(functionAddress)) {
                    tps.put(functionAddress, new TestpathsOfAFunction());
                }

                Offset offset = new Offset();
                offset.startingOffsetInFunction = Utils.toInt(getValue(testpath, IFunctionInstrumentationGeneration.START_OFFSET_IN_FUNCTION));
                offset.endOffsetInFunction = Utils.toInt(getValue(testpath, IFunctionInstrumentationGeneration.END_OFFSET_IN_FUNCTION));
                offset.startingOffsetInSourcecodeFile = Utils.toInt(getValue(testpath, IFunctionInstrumentationGeneration.START_OFFSET_IN_SOURCE_CODE_FILE));
                offset.endOffsetInSourcecodeFile = Utils.toInt(getValue(testpath, IFunctionInstrumentationGeneration.END_OFFSET_IN_SOURCE_CODE_FILE));
                offset.testpath = testpath;

                if (testCaseBegin != null) {
                    if (!tps.get(functionAddress).testpaths.contains(testCaseBegin)) {
                        tps.get(functionAddress).testpaths.add(testCaseBegin);
                    }
                }

                tps.get(functionAddress).testpaths.add(offset);
            }
        }
        return tps;
    }

	protected abstract int getNumberOfBasisPath(INode consideredSourcecodeNode);

    public static String getValue(String line, String property) {
        if (line.contains(IFunctionInstrumentationGeneration.DELIMITER_BETWEEN_PROPERTIES)) {
            String[] tokens = line.split(IFunctionInstrumentationGeneration.DELIMITER_BETWEEN_PROPERTIES);
            for (String token : tokens)
                if (token.split(IFunctionInstrumentationGeneration.DELIMITER_BETWEEN_PROPERTY_AND_VALUE)[0].equals(property))
                    return token.split(IFunctionInstrumentationGeneration.DELIMITER_BETWEEN_PROPERTY_AND_VALUE)[1];
        }
        return null;
    }

	protected abstract int getNumberofBranches(INode consideredSourcecodeNode);

	protected abstract int getNumberofStatements(INode consideredSourcecodeNode);

	protected abstract int getNumberofMcdcs(INode consideredSourcecodeNode);

	public void setTestpathContent(String testpathContent) {
		this.testpathContent = testpathContent;
	}

	public String getTestpathContent() {
		return testpathContent;
	}

	public void setConsideredSourcecodeNode(INode consideredSourcecodeNode) {
		this.consideredSourcecodeNode = consideredSourcecodeNode;
	}

	public INode getConsideredSourcecodeNode() {
		return consideredSourcecodeNode;
	}

	public String getCoverage() {
		return coverage;
	}

	public void setCoverage(String coverage) {
		this.coverage = coverage;
	}

	public int getNumberOfInstructions() {
		return numberOfInstructions;
	}

	public void setNumberOfInstructions(int numberOfInstructions) {
		this.numberOfInstructions = numberOfInstructions;
	}

	public int getNumberOfVisitedInstructions() {
		return numberOfVisitedInstructions;
	}

	public void setNumberOfVisitedInstructions(int numberOfVisitedInstructions) {
		this.numberOfVisitedInstructions = numberOfVisitedInstructions;
	}

	public List<ICFG> getAllCFG() {
		return allCFG;
	}

	public void setAllCFG(List<ICFG> allCFG) {
		this.allCFG = allCFG;
	}

    static class TestpathsOfAFunction {
        List<TestPathItem> testpaths = new ArrayList<>();

        @Override
        public String toString() {
            return testpaths.toString();
        }

        String[] getTestpathsInArray() {
            String[] tpInArray = new String[testpaths.size()];
            int count = 0;
            for (TestPathItem offset : testpaths) {
                tpInArray[count] = offset.testpath;
                count++;
            }
            return tpInArray;
        }
    }

    static abstract class TestPathItem {
        String testpath;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TestPathItem that = (TestPathItem) o;

            return testpath != null ? testpath.equals(that.testpath) : that.testpath == null;
        }
    }

    static abstract class TestCaseItem extends TestPathItem {
    }

    static class TestCaseBegin extends TestCaseItem {
        @Override
        public String toString() {
            return testpath.substring(TestPathUtils.BEGIN_TAG.length());
        }
    }

    static class Offset extends TestPathItem {
        int startingOffsetInSourcecodeFile;
        int endOffsetInSourcecodeFile;
        int startingOffsetInFunction;
        int endOffsetInFunction;

		@Override
		public String toString() {
			return "offset in source code file = " + startingOffsetInSourcecodeFile + ":" + endOffsetInSourcecodeFile + ", offset in function = " + startingOffsetInFunction + ":" + endOffsetInFunction + "\n";
		}
	}
}
