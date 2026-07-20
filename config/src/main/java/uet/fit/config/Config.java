package uet.fit.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Config extends AbstractConfig {
	static final @NotNull Config INSTANCE = new Config();

	private Config() {
	}


	public static @NotNull String getGppPath() {
		return INSTANCE.get("GPP_PATH", "/usr/bin/g++");
	}

	public static void setGppPath(@Nullable String value) {
		INSTANCE.put("GPP_PATH", value);
	}


	public static @NotNull String getQmakePath() {
		return INSTANCE.get("QMAKE_PATH", "/usr/bin/qmake");
	}

	public static void setQmakePath(@Nullable String value) {
		INSTANCE.put("QMAKE_PATH", value);
	}


	public static @NotNull String getMakePath() {
		return INSTANCE.get("MAKE_PATH", "/usr/bin/make");
	}

	public static void setMakePath(@Nullable String value) {
		INSTANCE.put("MAKE_PATH", value);
	}

	// =====

	public static @NotNull String getHomePath() {
		return INSTANCE.get("CIAUT_HOME", "");
	}

	public static void setHomePath(@Nullable String value) {
		INSTANCE.put("CIAUT_HOME", value);
	}

	public static @NotNull String getLogPath() {
		return INSTANCE.get("TOMCAT_LOG", "/var/log/tomcat9");
	}

	public static void setLogPath(@Nullable String value) {
		INSTANCE.put("TOMCAT_LOG", value);
	}

	// =====

	public static @NotNull String getSqlHost() {
		return INSTANCE.get("SQL_HOST", "localhost");
	}

	public static void setSqlHost(@Nullable String value) {
		INSTANCE.put("SQL_HOST", value);
	}


	public static int getSqlPort() {
		return INSTANCE.get("SQL_PORT", 3306);
	}

	public static void setSqlPort(int value) {
		INSTANCE.put("SQL_PORT", value);
	}


	public static @NotNull String getSqlUser() {
		return INSTANCE.get("SQL_USER", "root");
	}

	public static void setSqlUser(@Nullable String value) {
		INSTANCE.put("SQL_USER", value);
	}


	public static @NotNull String getSqlPassword() {
		return INSTANCE.get("SQL_PASSWORD", "password");
	}

	public static void setSqlPassword(@Nullable String value) {
		INSTANCE.put("SQL_PASSWORD", value);
	}

	// =====

	public static void setRunTestTimeout(int value) {
		INSTANCE.put("RUN_TEST_TIMEOUT", value);
	}

	public static int getRunTestTimeout() {
		return INSTANCE.get("RUN_TEST_TIMEOUT", 5);
	}

	// =====

	public static void setMakeJobsCount(int value) {
		INSTANCE.put("MAKE_JOBS_COUNT", value);
	}

	public static int getMakeJobsCount() {
		return INSTANCE.get("MAKE_JOBS_COUNT", 1);
	}

	// =====

	public static void setFunctionCallAnalyze(boolean value) {
		INSTANCE.put("FUNCTION_CALL_ANALYZE", value);
	}

	public static boolean isFunctionCallAnalyze() {
		return INSTANCE.get("FUNCTION_CALL_ANALYZE", false);
	}
}
