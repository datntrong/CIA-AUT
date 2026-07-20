package uet.fit.aut.env.build_result;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uet.fit.aut.execution.result_trace.AssertionResult;
import uet.fit.aut.testcase.TestCase;

import java.time.LocalDateTime;

public class Execution extends BuildResult {

	@NotNull
	private String testDriver;

	@NotNull
	private String testPath;

	@NotNull
	private String resultPath;

	@NotNull
	private AssertionResult assertionResult = new AssertionResult();

	@Nullable
	private String coverage;

	@NotNull
	private final TestCase testCase;

	@NotNull
	private LocalDateTime startTime, endTime;

	private @Nullable String log;

	public Execution(@NotNull TestCase testCase, String exePath) {
		super(exePath);
		this.testCase = testCase;
	}

	public void setLog(@Nullable String log) {
		this.log = log;
	}

	@Nullable
	public String getLog() {
		return log;
	}

	public @NotNull TestCase getTestCase() {
		return testCase;
	}

	public void setStartTime(@NotNull LocalDateTime startTime) {
		this.startTime = startTime;
	}

	public void setEndTime(@NotNull LocalDateTime endTime) {
		this.endTime = endTime;
	}

	public @NotNull LocalDateTime getEndTime() {
		return endTime;
	}

	public @NotNull LocalDateTime getStartTime() {
		return startTime;
	}

	public @NotNull String getTestDriver() {
		return testDriver;
	}

	public void setTestDriver(@NotNull String testDriver) {
		this.testDriver = testDriver;
	}

	public @NotNull String getTestPath() {
		return testPath;
	}

	public void setTestPath(@NotNull String testPath) {
		this.testPath = testPath;
	}

	public @NotNull String getResultPath() {
		return resultPath;
	}

	public void setResultPath(@NotNull String resultPath) {
		this.resultPath = resultPath;
	}

	public @NotNull AssertionResult getAssertionResult() {
		return assertionResult;
	}

	public void setAssertionResult(@NotNull AssertionResult assertionResult) {
		this.assertionResult = assertionResult;
	}

	public @NotNull String getCoverage() {
		return coverage;
	}

	public void setCoverage(@NotNull String coverage) {
		this.coverage = coverage;
	}
}
