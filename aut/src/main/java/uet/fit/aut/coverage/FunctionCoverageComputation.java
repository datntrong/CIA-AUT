package uet.fit.aut.coverage;

import uet.fit.aut.autogen.cfg.ICFG;
import uet.fit.aut.env.CoverageType;
import uet.fit.aut.parser.obj.AbstractFunctionNode;
import uet.fit.aut.parser.obj.ICommonFunctionNode;
import uet.fit.aut.parser.obj.IFunctionNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.util.CFGUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is used to compute coverage of function level
 * <p>
 * The type of coverage is only STATEMENT, BRANCH, and MCDC.
 * <p>
 * For STATEMENT+BRANCH, STATEMENT+MCDC, these kinds of coverage include two coverage types.
 */
public class FunctionCoverageComputation extends AbstractCoverageComputation {

	protected ICommonFunctionNode functionNode;

	public void compute() {
		if (functionNode == null
				|| !(functionNode instanceof IFunctionNode)
				|| testpathContent == null || testpathContent.length() == 0) {
			this.numberOfInstructions = getNumberOfInstructions(functionNode, coverage);
			return;
		}
		if (coverage.equals(CoverageType.STATEMENT_AND_BRANCH) ||
				coverage.equals(CoverageType.STATEMENT_AND_MCDC))
			return;

		Map<String, TestpathsOfAFunction> affectedFunctions = categoryTestpathByFunctionPath(testpathContent.split("\n"), coverage);
		affectedFunctions = removeRedundantTestpath(affectedFunctions);

		int nInstructions = getNumberOfInstructions(functionNode, coverage);

		int nVisitedInstructions;
		nVisitedInstructions = getNumberOfVisitedInstructions(affectedFunctions, coverage, consideredSourcecodeNode, allCFG);

		this.numberOfInstructions = nInstructions;
		this.numberOfVisitedInstructions = nVisitedInstructions;
	}

	protected Map<String, TestpathsOfAFunction> removeRedundantTestpath(Map<String, TestpathsOfAFunction> affectedFunctions){
		Map<String, TestpathsOfAFunction> output = new HashMap<>();
		String path = functionNode.getAbsolutePath();
		output.put(path, affectedFunctions.get(path));
		return output;
	}

	protected int getNumberofMcdcs(INode functionNode) {
		int nMcdcs = 0;
		if (functionNode instanceof AbstractFunctionNode) {
			ICFG cfg = CFGUtils.createCFG((IFunctionNode) functionNode, CoverageType.MCDC);
			if (cfg != null) {
				nMcdcs += cfg.getVisitedBranches().size() + cfg.getUnvisitedBranches().size();
			}
		}

		return nMcdcs;
	}

	protected int getNumberofBranches(INode functionNode) {
		int nBranches = 0;
		if (functionNode instanceof AbstractFunctionNode) {
			ICFG cfg = CFGUtils.createCFG((IFunctionNode) functionNode, CoverageType.BRANCH);
			if (cfg != null) {
				nBranches += cfg.getVisitedBranches().size() + cfg.getUnvisitedBranches().size();
			}
		}

		return nBranches;
	}

	protected int getNumberofStatements(INode functionNode) {
		int nStatements = 0;
		if (functionNode instanceof AbstractFunctionNode) {
			ICFG cfg = CFGUtils.createCFG((IFunctionNode) functionNode, CoverageType.STATEMENT);
			if (cfg != null) {
				nStatements += cfg.getVisitedStatements().size() + cfg.getUnvisitedStatements().size();
			}
		}

		return nStatements;
	}

	@Override
	protected int getNumberOfBasisPath(INode functionNode) {
		int nBasicPaths = 0;
		if (functionNode instanceof AbstractFunctionNode) {
			ICFG cfg = CFGUtils.createCFG((IFunctionNode) functionNode, CoverageType.BASIS_PATH);
			if (cfg != null) {
				nBasicPaths += cfg.getVisitedBasisPaths().size() + cfg.getUnvisitedBasisPaths().size();
			}
		}

		return nBasicPaths;
	}

	public void setFunctionNode(ICommonFunctionNode functionNode) {
		this.functionNode = functionNode;
	}

	public ICommonFunctionNode getFunctionNode() {
		return functionNode;
	}
}
