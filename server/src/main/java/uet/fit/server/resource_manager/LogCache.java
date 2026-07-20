package uet.fit.server.resource_manager;

import uet.fit.dto.logger.RapidlyEntry;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class LogCache extends ConcurrentHashMap<String, LinkedBlockingQueue<RapidlyEntry>> {

	private static final int SIZE = 1024;

	private static LogCache instance;

	public static LogCache getInstance() {
		if (instance == null) {
			instance = new LogCache();
		}
		return instance;
	}

	public void register(String user) {
		put(user, new LinkedBlockingQueue<>());
	}

	public void cache(String user, RapidlyEntry log) {
		if (!containsKey(user)) {
			return;
		}

//		while (get(user).size() > SIZE) {
//			get(user).poll();
//		}

		get(user).offer(log);
	}
}
