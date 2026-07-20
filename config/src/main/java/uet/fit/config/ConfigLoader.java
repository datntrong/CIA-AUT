package uet.fit.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.nio.file.Path;

@WebListener
public final class ConfigLoader implements ServletContextListener {
	private static final @Nullable Path CONFIG_PATH = getConfig();
	private static final @NotNull Path RELATIVE_PATH = Path.of("WEB-INF/classes/config.properties");

	private static @Nullable Path getConfig() {
		try {
			final String propertyPath = System.getProperty("CIAUT_CONFIG");
			if (propertyPath != null) return Path.of(propertyPath);
			final String envPath = System.getenv("CIAUT_CONFIG");
			return envPath != null ? Path.of(envPath) : null;
		} catch (final Exception ignored) {
		}
		return null;
	}

	@Override
	public void contextInitialized(@NotNull ServletContextEvent event) {
		final Path configPath = CONFIG_PATH != null
				? CONFIG_PATH
				: Path.of(event.getServletContext().getRealPath("/")).resolve(RELATIVE_PATH);
		Config.INSTANCE.load(configPath);
	}

	@Override
	public void contextDestroyed(@NotNull ServletContextEvent event) {
		final Path configPath = CONFIG_PATH != null
				? CONFIG_PATH
				: Path.of(event.getServletContext().getRealPath("/")).resolve(RELATIVE_PATH);
		Config.INSTANCE.save(configPath);
	}
}
