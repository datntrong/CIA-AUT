package uet.fit.client.logger;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WaitingLogs extends ConcurrentHashMap<UUID, Long> {

	private static WaitingLogs instance;

	public static WaitingLogs getInstance() {
		if (instance == null)
			instance = new WaitingLogs();

		return instance;
	}
}
