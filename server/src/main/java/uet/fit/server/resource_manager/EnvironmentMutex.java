package uet.fit.server.resource_manager;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EnvironmentMutex {

	private static EnvironmentMutex instance;

	public static EnvironmentMutex getInstance() {
		if (instance == null)
			instance = new EnvironmentMutex();
		return instance;
	}

	private final Map<String, Boolean> map = new ConcurrentHashMap<>();

	/**
	 * Attempt to lock the current thread with pro path
	 * @param proPath corresponding to project
	 * @return true if project hasn't been locked yet
	 * 		   false if project has already been locked
	 */
	public boolean lock(@NotNull String proPath) {
		if (map.containsKey(proPath))
			return false;
		else {
			map.put(proPath, true);
			return true;
		}
	}

	/**
	 * Wait the current thread until project unlocked
	 */
	public void wait(@NotNull String proPath) {
		while (map.containsKey(proPath)) {
			// wait
		}
	}

	/**
	 * Unlock project
	 */
	public void unlock(@NotNull String proPath) {
		map.remove(proPath);
	}
}
