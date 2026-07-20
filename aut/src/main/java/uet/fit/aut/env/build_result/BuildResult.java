package uet.fit.aut.env.build_result;

public abstract class BuildResult {

	private final String exePath;

	public BuildResult(String exePath) {
		this.exePath = exePath;
	}
}
