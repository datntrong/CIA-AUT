package uet.fit.aut.coverage;

import uet.fit.aut.env.CoverageType;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.parser.obj.ISourcecodeFileNode;

import java.io.File;
import java.util.Map;

/**
 * This class is used to compute coverage of source code file.
 *
 * The type of coverage is only STATEMENT, BRANCH, and MCDC.
 *
 * For STATEMENT+BRANCH, STATEMENT+MCDC, these kinds of coverage include two coverage types.
 */
public class SourcecodeCoverageComputation extends AbstractCoverageComputation{

	public void compute() {
		if (consideredSourcecodeNode == null || !(new File(consideredSourcecodeNode.getAbsolutePath())).exists()
				|| !(consideredSourcecodeNode instanceof ISourcecodeFileNode)
				|| testpathContent == null || testpathContent.length() == 0) {
			this.numberOfInstructions = getNumberOfInstructions(consideredSourcecodeNode, coverage);
			return;
		}
		if (coverage.equals(CoverageType.STATEMENT_AND_BRANCH) ||
				coverage.equals(CoverageType.STATEMENT_AND_MCDC))
			return;

		Map<String, TestpathsOfAFunction> affectedFunctions = categoryTestpathByFunctionPath(testpathContent.split("\n"), coverage);
		affectedFunctions = removeRedundantTestpath(affectedFunctions);

		int nInstructions = getNumberOfInstructions(consideredSourcecodeNode, coverage);

		int nVisitedInstructions = getNumberOfVisitedInstructions(affectedFunctions, coverage, consideredSourcecodeNode, allCFG);

		this.numberOfInstructions = nInstructions;
		this.numberOfVisitedInstructions = nVisitedInstructions;
	}

	protected Map<String, TestpathsOfAFunction> removeRedundantTestpath(Map<String, TestpathsOfAFunction> affectedFunctions){
		return affectedFunctions;
	}

	@Override
	protected int getNumberofBranches(INode consideredSourcecodeNode) {
		return InstructionComputation.count(consideredSourcecodeNode, CoverageType.BRANCH);
	}

	@Override
	protected int getNumberofStatements(INode consideredSourcecodeNode) {
		return InstructionComputation.count(consideredSourcecodeNode, CoverageType.STATEMENT);
	}

	@Override
	protected int getNumberofMcdcs(INode consideredSourcecodeNode) {
		return InstructionComputation.count(consideredSourcecodeNode, CoverageType.MCDC);
	}

	@Override
	protected int getNumberOfBasisPath(INode consideredSourcecodeNode) {
		return InstructionComputation.count(consideredSourcecodeNode, CoverageType.BASIS_PATH);
	}
}

