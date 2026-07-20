package uet.fit.client.utils;

import java.nio.file.Path;

public interface ConfigLocation {
	String BASE_DIR = Path.of(System.getProperty("user.home"), ".ciaut").toString();
	String SERVER_URL_FILE = Path.of(ConfigLocation.BASE_DIR, "server.init").toString();
}
