package uet.fit.server.logger;

import uet.fit.aut.logger.ExternalLogger;
import uet.fit.dto.logger.LogDTO;

public class CoreLogger implements ExternalLogger {

	private final String username;
	private final LogDTO.Position position;

	public CoreLogger(String username, LogDTO.Position position) {
		this.username = username;
		this.position = position;
	}

	@Override
	public void log(String message) {
		ServerLogger.debug(username, position, message);
	}

	@Override
	public void info(String message) {
		ServerLogger.info(username, position, message);

	}

	@Override
	public void error(String message) {
		ServerLogger.error(username, position, message);
	}

	@Override
	public void progress(String id, String title, int current, int total) {
		ServerLogger.progress(username, id, title, current, total);
	}
}