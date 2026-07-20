package uet.fit.server.logger.cia;

import org.apache.commons.io.output.StringBuilderWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class LogBuilder implements AutoCloseable {
	private static final @NotNull String LOG_LEVEL_TRACE = "T";
	private static final @NotNull String LOG_LEVEL_DEBUG = "D";
	private static final @NotNull String LOG_LEVEL_INFO = "I";
	private static final @NotNull String LOG_LEVEL_WARN = "W";
	private static final @NotNull String LOG_LEVEL_ERROR = "E";

	private static final @NotNull DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

	private final @NotNull StringBuilder messageBuilder = new StringBuilder();
	private volatile boolean open = true;


	public @NotNull String getLog() {
		synchronized (messageBuilder) {
			final String message = messageBuilder.toString();
			messageBuilder.setLength(0);
			return message;
		}
	}

	public boolean isOpen() {
		return open;
	}

	@Override
	public void close() {
		this.open = false;
	}

	public @NotNull Logger wrap(@NotNull Logger logger) {
		return new WrappingLogger(logger);
	}


	private final class WrappingLogger implements Logger {
		private final @NotNull Logger logger;


		WrappingLogger(@NotNull Logger logger) {
			this.logger = logger;
		}


		private void writeLog(@NotNull String level,
				@Nullable String message, @Nullable Throwable throwable) {
			// Append date-time, current thread id, log level, message
			final StringBuilder builder = new StringBuilder(128)
					.append(FORMATTER.format(LocalDateTime.now()))
					.append(" [").append(Thread.currentThread().getId())
					.append("] [").append(level)
					.append("] ").append(logger.getName());

			if (message != null) {
				builder.append(" - ").append(message);
			}

			builder.append('\n');

			if (throwable != null) {
				try (final PrintWriter printWriter = new PrintWriter(new StringBuilderWriter(builder))) {
					throwable.printStackTrace(printWriter);
				}
				builder.append('\n');
			}

			synchronized (messageBuilder) {
				messageBuilder.append(builder);
			}
		}

		private void formatAndLog(@NotNull String level, @Nullable String format,
				@Nullable Object argumentA, @Nullable Object argumentB) {
			final FormattingTuple tuple = MessageFormatter.format(format, argumentA, argumentB);
			writeLog(level, tuple.getMessage(), tuple.getThrowable());
		}

		private void formatAndLog(@NotNull String level, @Nullable String format,
				@Nullable Object @Nullable ... arguments) {
			final FormattingTuple tuple = MessageFormatter.arrayFormat(format, arguments);
			writeLog(level, tuple.getMessage(), tuple.getThrowable());
		}


		@Override
		public String getName() {
			return logger.getName();
		}

		@Override
		public boolean isTraceEnabled() {
			return logger.isTraceEnabled();
		}

		@Override
		public void trace(@Nullable String message) {
			logger.trace(message);
			if (open) writeLog(LOG_LEVEL_TRACE, message, null);
		}

		@Override
		public void trace(@Nullable String format, @Nullable Object argument) {
			logger.trace(format, argument);
			if (open) formatAndLog(LOG_LEVEL_TRACE, format, argument, null);
		}

		@Override
		public void trace(@Nullable String format, @Nullable Object argumentA, @Nullable Object argumentB) {
			logger.trace(format, argumentA, argumentB);
			if (open) formatAndLog(LOG_LEVEL_TRACE, format, argumentA, argumentB);
		}

		@Override
		public void trace(@Nullable String format, @Nullable Object @Nullable ... arguments) {
			logger.trace(format, arguments);
			if (open) formatAndLog(LOG_LEVEL_TRACE, format, arguments);
		}

		@Override
		public void trace(@Nullable String message, @Nullable Throwable throwable) {
			logger.trace(message, throwable);
			if (open) writeLog(LOG_LEVEL_TRACE, message, throwable);
		}

		@Override
		public boolean isTraceEnabled(@Nullable Marker marker) {
			return logger.isTraceEnabled(marker);
		}

		@Override
		public void trace(@Nullable Marker marker, @Nullable String message) {
			logger.trace(marker, message);
			if (open) writeLog(LOG_LEVEL_TRACE, message, null);
		}

		@Override
		public void trace(@Nullable Marker marker, @Nullable String format, @Nullable Object argument) {
			logger.trace(marker, format, argument);
			if (open) formatAndLog(LOG_LEVEL_TRACE, format, argument, null);
		}

		@Override
		public void trace(@Nullable Marker marker, @Nullable String format, @Nullable Object argumentA, @Nullable Object argumentB) {
			logger.trace(marker, format, argumentA, argumentB);
			if (open) formatAndLog(LOG_LEVEL_TRACE, format, argumentA, argumentB);
		}

		@Override
		public void trace(@Nullable Marker marker, @Nullable String format, @Nullable Object @Nullable ... arguments) {
			logger.trace(marker, format, arguments);
			if (open) formatAndLog(LOG_LEVEL_TRACE, format, arguments);
		}

		@Override
		public void trace(@Nullable Marker marker, @Nullable String message, @Nullable Throwable throwable) {
			logger.trace(marker, message, throwable);
			if (open) writeLog(LOG_LEVEL_TRACE, message, throwable);
		}

		@Override
		public boolean isDebugEnabled() {
			return logger.isDebugEnabled();
		}

		@Override
		public void debug(@Nullable String message) {
			logger.debug(message);
			if (open) writeLog(LOG_LEVEL_DEBUG, message, null);
		}

		@Override
		public void debug(@Nullable String format, @Nullable Object argument) {
			logger.debug(format, argument);
			if (open) formatAndLog(LOG_LEVEL_DEBUG, format, argument, null);
		}

		@Override
		public void debug(@Nullable String format, @Nullable Object argumentA, @Nullable Object argumentB) {
			logger.debug(format, argumentA, argumentB);
			if (open) formatAndLog(LOG_LEVEL_DEBUG, format, argumentA, argumentB);
		}

		@Override
		public void debug(@Nullable String format, @Nullable Object @Nullable ... arguments) {
			logger.debug(format, arguments);
			if (open) formatAndLog(LOG_LEVEL_DEBUG, format, arguments);
		}

		@Override
		public void debug(@Nullable String message, @Nullable Throwable throwable) {
			logger.debug(message, throwable);
			if (open) writeLog(LOG_LEVEL_DEBUG, message, throwable);
		}

		@Override
		public boolean isDebugEnabled(@Nullable Marker marker) {
			return logger.isDebugEnabled(marker);
		}

		@Override
		public void debug(@Nullable Marker marker, @Nullable String message) {
			logger.debug(marker, message);
			if (open) writeLog(LOG_LEVEL_DEBUG, message, null);
		}

		@Override
		public void debug(@Nullable Marker marker, @Nullable String format, @Nullable Object argument) {
			logger.debug(marker, format, argument);
			if (open) formatAndLog(LOG_LEVEL_DEBUG, format, argument, null);
		}

		@Override
		public void debug(@Nullable Marker marker, @Nullable String format, @Nullable Object argumentA, @Nullable Object argumentB) {
			logger.debug(marker, format, argumentA, argumentB);
			if (open) formatAndLog(LOG_LEVEL_DEBUG, format, argumentA, argumentB);
		}

		@Override
		public void debug(@Nullable Marker marker, @Nullable String format, @Nullable Object @Nullable ... arguments) {
			logger.debug(marker, format, arguments);
			if (open) formatAndLog(LOG_LEVEL_DEBUG, format, arguments);
		}

		@Override
		public void debug(@Nullable Marker marker, @Nullable String message, @Nullable Throwable throwable) {
			logger.debug(marker, message, throwable);
			if (open) writeLog(LOG_LEVEL_DEBUG, message, throwable);
		}

		@Override
		public boolean isInfoEnabled() {
			return logger.isInfoEnabled();
		}

		@Override
		public void info(@Nullable String message) {
			logger.info(message);
			if (open) writeLog(LOG_LEVEL_INFO, message, null);
		}

		@Override
		public void info(@Nullable String format, @Nullable Object argument) {
			logger.info(format, argument);
			if (open) formatAndLog(LOG_LEVEL_INFO, format, argument, null);
		}

		@Override
		public void info(@Nullable String format, @Nullable Object argumentA, @Nullable Object argumentB) {
			logger.info(format, argumentA, argumentB);
			if (open) formatAndLog(LOG_LEVEL_INFO, format, argumentA, argumentB);
		}

		@Override
		public void info(@Nullable String format, @Nullable Object @Nullable ... arguments) {
			logger.info(format, arguments);
			if (open) formatAndLog(LOG_LEVEL_INFO, format, arguments);
		}

		@Override
		public void info(@Nullable String message, @Nullable Throwable throwable) {
			logger.info(message, throwable);
			if (open) writeLog(LOG_LEVEL_INFO, message, throwable);
		}

		@Override
		public boolean isInfoEnabled(@Nullable Marker marker) {
			return logger.isInfoEnabled(marker);
		}

		@Override
		public void info(@Nullable Marker marker, @Nullable String message) {
			logger.info(marker, message);
			if (open) writeLog(LOG_LEVEL_INFO, message, null);
		}

		@Override
		public void info(@Nullable Marker marker, @Nullable String format, @Nullable Object argument) {
			logger.info(marker, format, argument);
			if (open) formatAndLog(LOG_LEVEL_INFO, format, argument, null);
		}

		@Override
		public void info(@Nullable Marker marker, @Nullable String format, @Nullable Object argumentA, @Nullable Object argumentB) {
			logger.info(marker, format, argumentA, argumentB);
			if (open) formatAndLog(LOG_LEVEL_INFO, format, argumentA, argumentB);
		}

		@Override
		public void info(@Nullable Marker marker, @Nullable String format, @Nullable Object @Nullable ... arguments) {
			logger.info(marker, format, arguments);
			if (open) formatAndLog(LOG_LEVEL_INFO, format, arguments);
		}

		@Override
		public void info(@Nullable Marker marker, @Nullable String message, @Nullable Throwable throwable) {
			logger.info(marker, message, throwable);
			if (open) writeLog(LOG_LEVEL_INFO, message, throwable);
		}

		@Override
		public boolean isWarnEnabled() {
			return logger.isWarnEnabled();
		}

		@Override
		public void warn(@Nullable String message) {
			logger.warn(message);
			if (open) writeLog(LOG_LEVEL_WARN, message, null);
		}

		@Override
		public void warn(@Nullable String format, @Nullable Object argument) {
			logger.warn(format, argument);
			if (open) formatAndLog(LOG_LEVEL_WARN, format, argument, null);
		}

		@Override
		public void warn(@Nullable String format, @Nullable Object argumentA, @Nullable Object argumentB) {
			logger.warn(format, argumentA, argumentB);
			if (open) formatAndLog(LOG_LEVEL_WARN, format, argumentA, argumentB);
		}

		@Override
		public void warn(@Nullable String format, @Nullable Object @Nullable ... arguments) {
			logger.warn(format, arguments);
			if (open) formatAndLog(LOG_LEVEL_WARN, format, arguments);
		}

		@Override
		public void warn(@Nullable String message, @Nullable Throwable throwable) {
			logger.warn(message, throwable);
			if (open) writeLog(LOG_LEVEL_WARN, message, throwable);
		}

		@Override
		public boolean isWarnEnabled(@Nullable Marker marker) {
			return logger.isWarnEnabled(marker);
		}

		@Override
		public void warn(@Nullable Marker marker, @Nullable String message) {
			logger.warn(marker, message);
			if (open) writeLog(LOG_LEVEL_WARN, message, null);
		}

		@Override
		public void warn(@Nullable Marker marker, @Nullable String format, @Nullable Object argument) {
			logger.warn(marker, format, argument);
			if (open) formatAndLog(LOG_LEVEL_WARN, format, argument, null);
		}

		@Override
		public void warn(@Nullable Marker marker, @Nullable String format, @Nullable Object argumentA, @Nullable Object argumentB) {
			logger.warn(marker, format, argumentA, argumentB);
			if (open) formatAndLog(LOG_LEVEL_WARN, format, argumentA, argumentB);
		}

		@Override
		public void warn(@Nullable Marker marker, @Nullable String format, @Nullable Object @Nullable ... arguments) {
			logger.warn(marker, format, arguments);
			if (open) formatAndLog(LOG_LEVEL_WARN, format, arguments);
		}

		@Override
		public void warn(@Nullable Marker marker, @Nullable String message, @Nullable Throwable throwable) {
			logger.warn(marker, message, throwable);
			if (open) writeLog(LOG_LEVEL_WARN, message, throwable);
		}

		@Override
		public boolean isErrorEnabled() {
			return logger.isErrorEnabled();
		}

		@Override
		public void error(@Nullable String message) {
			logger.error(message);
			if (open) writeLog(LOG_LEVEL_ERROR, message, null);
		}

		@Override
		public void error(@Nullable String format, @Nullable Object argument) {
			logger.error(format, argument);
			if (open) formatAndLog(LOG_LEVEL_ERROR, format, argument, null);
		}

		@Override
		public void error(@Nullable String format, @Nullable Object argumentA, @Nullable Object argumentB) {
			logger.error(format, argumentA, argumentB);
			if (open) formatAndLog(LOG_LEVEL_ERROR, format, argumentA, argumentB);
		}

		@Override
		public void error(@Nullable String format, @Nullable Object @Nullable ... arguments) {
			logger.error(format, arguments);
			if (open) formatAndLog(LOG_LEVEL_ERROR, format, arguments);
		}

		@Override
		public void error(@Nullable String message, @Nullable Throwable throwable) {
			logger.error(message, throwable);
			if (open) writeLog(LOG_LEVEL_ERROR, message, throwable);
		}

		@Override
		public boolean isErrorEnabled(@Nullable Marker marker) {
			return logger.isErrorEnabled(marker);
		}

		@Override
		public void error(@Nullable Marker marker, @Nullable String message) {
			logger.error(marker, message);
			if (open) writeLog(LOG_LEVEL_ERROR, message, null);
		}

		@Override
		public void error(@Nullable Marker marker, @Nullable String format, @Nullable Object argument) {
			logger.error(marker, format, argument);
			if (open) formatAndLog(LOG_LEVEL_ERROR, format, argument, null);
		}

		@Override
		public void error(@Nullable Marker marker, @Nullable String format, @Nullable Object argumentA, @Nullable Object argumentB) {
			logger.error(marker, format, argumentA, argumentB);
			if (open) formatAndLog(LOG_LEVEL_ERROR, format, argumentA, argumentB);
		}

		@Override
		public void error(@Nullable Marker marker, @Nullable String format, @Nullable Object @Nullable ... arguments) {
			logger.error(marker, format, arguments);
			if (open) formatAndLog(LOG_LEVEL_ERROR, format, arguments);
		}

		@Override
		public void error(@Nullable Marker marker, @Nullable String message, @Nullable Throwable throwable) {
			logger.error(marker, message, throwable);
			if (open) writeLog(LOG_LEVEL_ERROR, message, throwable);
		}
	}
}
