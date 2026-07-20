package uet.fit.server.resource_manager;

import java.util.concurrent.ConcurrentHashMap;

public class ProjectUsers extends ConcurrentHashMap<String, String> {

	private static ProjectUsers instance;

	public static ProjectUsers getInstance() {
		if (instance == null)
			instance = new ProjectUsers();
		return instance;
	}

	private ProjectUsers() {

	}
}
