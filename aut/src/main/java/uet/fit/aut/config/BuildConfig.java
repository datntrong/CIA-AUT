package uet.fit.aut.config;

import uet.fit.config.Config;

public class BuildConfig implements IBuildConfig {

	private static final int JOBS = 1;

	private String qmake;

	private String makeClean;

	private String make;

	private BuildConfig() {

	}

	public String getMake() {
		return make;
	}

	public String getMakeClean() {
		return makeClean;
	}

	public String getQmake() {
		return qmake;
	}

	public static BuildConfig load() {
		BuildConfig buildConfig = new BuildConfig();
		buildConfig.qmake = Config.getQmakePath();
		String makePath = Config.getMakePath();
		buildConfig.makeClean = String.format("%s clean", makePath);
		buildConfig.make = String.format("%s -j%d", makePath, JOBS);
		return buildConfig;
	}
}
