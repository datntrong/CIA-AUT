package uet.fit.aut.logger;

public interface ExternalLogger {

	void log(String message);
	void info(String message);
	void error(String message);
	void progress(String id, String title, int current, int total);

	static void log(ExternalLogger logger, String message) {
		if (logger != null)
			logger.log(message);
	}

	static void info(ExternalLogger logger, String message) {
		if (logger != null)
			logger.info(message);
	}

	static void error(ExternalLogger logger, String message) {
		if (logger != null)
			logger.error(message);
	}

	static void registerTask(ExternalLogger logger, String id, String title, int total) {
		progress(logger, id, title, 0, total);
	}

	static void progress(ExternalLogger logger, String id, String title, int current, int total) {
		if (logger != null) {
			logger.progress(id, title, current, total);
		}
	}
}
