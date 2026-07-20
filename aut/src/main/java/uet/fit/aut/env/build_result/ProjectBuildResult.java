package uet.fit.aut.env.build_result;

import uet.fit.aut.config.ProjectConfig;

public class ProjectBuildResult extends BuildResult {

	private final ProjectConfig projectConfig;

	public ProjectBuildResult(String exePath, ProjectConfig projectConfig) {
		super(exePath);
		this.projectConfig = projectConfig;
	}


	public ProjectConfig getProjectConfig() {
		return projectConfig;
	}
}
