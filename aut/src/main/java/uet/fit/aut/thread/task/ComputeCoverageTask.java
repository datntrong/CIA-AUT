package uet.fit.aut.thread.task;

import uet.fit.aut.coverage.CoverageData;
import uet.fit.aut.coverage.CoverageManager;
import uet.fit.aut.testcase.TestCase;

import java.util.List;

public class ComputeCoverageTask extends AbstractAUTTask<CoverageData> {

	private final List<TestCase> testCases;
	private final String coverageType;

	public ComputeCoverageTask(String coverageType, List<TestCase> testCases) {
		this.coverageType = coverageType;
		this.testCases = testCases;
	}

	@Override
	public CoverageData run() {
		return CoverageManager.getCoverageAtFileLevel(testCases, coverageType);
	}
}
