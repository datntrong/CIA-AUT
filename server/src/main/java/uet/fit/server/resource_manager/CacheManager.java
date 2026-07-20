package uet.fit.server.resource_manager;

import java.util.concurrent.ConcurrentHashMap;

public class CacheManager extends ConcurrentHashMap<String, CacheData> {

	private static CacheManager instance;

	private CacheManager() {
	}

	public static CacheManager getInstance() {
		if (instance == null) {
			instance = new CacheManager();
		}
		return instance;
	}
}
