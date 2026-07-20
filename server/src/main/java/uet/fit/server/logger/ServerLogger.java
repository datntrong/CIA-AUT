package uet.fit.server.logger;

import org.apache.log4j.Logger;
import uet.fit.dto.logger.LogDTO;
import uet.fit.dto.logger.ProgressDTO;
import uet.fit.server.resource_manager.LogCache;

public final class ServerLogger extends Logger {
	private ServerLogger(String name) {
		super(name);
	}

	public static ServerLogger get(Class<?> c) {
		return new ServerLogger(c.getName());
	}

	public static void info(String user, LogDTO.Position position, Object message) {
		long time = System.currentTimeMillis();
		LogDTO log = new LogDTO(LogDTO.TYPE_INF, position, message.toString(), time);
		LogCache.getInstance().cache(user, log);
	}

	public static void debug(String user, LogDTO.Position position, Object message) {
		long time = System.currentTimeMillis();
		LogDTO log = new LogDTO(LogDTO.TYPE_DEB, position, message.toString(), time);
		LogCache.getInstance().cache(user, log);
	}

	public static void error(String user, LogDTO.Position position, Object message) {
		long time = System.currentTimeMillis();
		LogDTO log = new LogDTO(LogDTO.TYPE_ERR, position, message.toString(), time);
		LogCache.getInstance().cache(user, log);
	}

	public static void progress(String user, String id, String title, int current, int total) {
		long time = System.currentTimeMillis();
		ProgressDTO progress = new ProgressDTO(id, title, current, total, time);
		LogCache.getInstance().cache(user, progress);
	}
}
