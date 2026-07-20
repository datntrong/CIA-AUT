package uet.fit.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

abstract class AbstractConfig {
	private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(AbstractConfig.class);
	private final @NotNull Properties properties = new Properties();


	void load(@NotNull Path path) {
		LOGGER.info("Loading config from " + path);
		try (final Reader reader = Files.newBufferedReader(path)) {
			properties.load(reader);
			LOGGER.info("Success loading config from " + path);
		} catch (final IOException exception) {
			LOGGER.warn("Failed loading config from " + path, exception);
		}
	}

	void save(@NotNull Path path) {
		LOGGER.info("Saving config from " + path);
		try (final Writer writer = Files.newBufferedWriter(path)) {
			properties.store(writer, "");
			LOGGER.warn("Success saving config to " + path);
		} catch (final IOException exception) {
			LOGGER.warn("Failed saving config to " + path, exception);
		}
	}


	protected @NotNull String get(@NotNull String key, @NotNull String defaultValue) {
		return properties.getProperty(key, defaultValue);
	}

	protected int get(@NotNull String key, int defaultValue) {
		final String value = properties.getProperty(key);
		if (value == null) return defaultValue;
		try {
			return Integer.parseInt(value);
		} catch (final NumberFormatException exception) {
			LOGGER.info("Failed parsing config key " + key + ", invalid value " + value);
			return defaultValue;
		}
	}

	protected long get(@NotNull String key, long defaultValue) {
		final String value = properties.getProperty(key);
		if (value == null) return defaultValue;
		try {
			return Long.parseLong(value);
		} catch (final NumberFormatException exception) {
			LOGGER.info("Failed parsing config key " + key + ", invalid value " + value);
			return defaultValue;
		}
	}

	protected boolean get(@NotNull String key, boolean defaultValue) {
		final String value = properties.getProperty(key);
		if (value == null) return defaultValue;
		try {
			return Boolean.parseBoolean(value);
		} catch (final NumberFormatException exception) {
			LOGGER.info("Failed parsing config key " + key + ", invalid value " + value);
			return defaultValue;
		}
	}

	protected float get(@NotNull String key, float defaultValue) {
		final String value = properties.getProperty(key);
		if (value == null) return defaultValue;
		try {
			return Float.parseFloat(value);
		} catch (final NumberFormatException exception) {
			LOGGER.info("Failed parsing config key " + key + ", invalid value " + value);
			return defaultValue;
		}
	}

	protected double get(@NotNull String key, double defaultValue) {
		final String value = properties.getProperty(key);
		if (value == null) return defaultValue;
		try {
			return Double.parseDouble(value);
		} catch (final NumberFormatException exception) {
			LOGGER.info("Failed parsing config key " + key + ", invalid value " + value);
			return defaultValue;
		}
	}


	protected void put(@NotNull String key, @Nullable String value) {
		if (value != null) {
			properties.setProperty(key, value);
		} else {
			properties.remove(key);
		}
	}

	protected void put(@NotNull String key, int value) {
		properties.setProperty(key, String.valueOf(value));
	}

	protected void put(@NotNull String key, long value) {
		properties.setProperty(key, String.valueOf(value));
	}

	protected void put(@NotNull String key, boolean value) {
		properties.setProperty(key, String.valueOf(value));
	}

	protected void put(@NotNull String key, float value) {
		properties.setProperty(key, String.valueOf(value));
	}

	protected void put(@NotNull String key, double value) {
		properties.setProperty(key, String.valueOf(value));
	}
}
