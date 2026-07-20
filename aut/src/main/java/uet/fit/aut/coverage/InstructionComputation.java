package uet.fit.aut.coverage;

import uet.fit.aut.autogen.cfg.ICFG;

import uet.fit.aut.env.CoverageType;
import uet.fit.aut.parser.obj.IFunctionNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.search.Search;
import uet.fit.aut.search.condition.AbstractFunctionNodeCondition;
import uet.fit.aut.util.CFGUtils;

import java.util.List;

/**
 * Compute the number of statements/branches
 */
public class InstructionComputation {

	public static int count(INode sourceNode, String coverageType) {
		// constructor, destructor, normal function
		List<INode> functionNodes = Search.searchNodes(sourceNode, new AbstractFunctionNodeCondition());

		int result = 0;

		for (INode node : functionNodes) {
			if (node instanceof IFunctionNode) {
				IFunctionNode functionNode = (IFunctionNode) node;
				result += computeNodeByType(functionNode, coverageType);
			}
		}

		return result;
	}

	private static int computeNodeByType(IFunctionNode functionNode, String coverageType) {
		ICFG cfg = CFGUtils.createCFG(functionNode, coverageType);

		switch (coverageType) {
			case CoverageType.BRANCH:
			case CoverageType.MCDC:
				return cfg.getVisitedBranches().size() + cfg.getUnvisitedBranches().size();

			case CoverageType.STATEMENT:
				return cfg.getVisitedStatements().size() + cfg.getUnvisitedStatements().size();

			case CoverageType.BASIS_PATH:
				return cfg.getVisitedBasisPaths().size() + cfg.getUnvisitedBasisPaths().size();

			default:
				return 0;
		}
	}
}

