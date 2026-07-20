package uet.fit.aut.thread.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.config.IBuildConfig;
import uet.fit.aut.exception.MakeCleanFailureException;
import uet.fit.aut.exception.MakeFailureException;
import uet.fit.aut.logger.ExternalLogger;
import uet.fit.aut.ter.OnExceptionListener;
import uet.fit.aut.ter.OnResponseListener;
import uet.fit.aut.ter.ProcessHandler;
import uet.fit.aut.thread.AutExecutors;
import uet.fit.aut.util.SpecialCharacter;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

public abstract class CompileTask<T> extends AbstractAUTTask<T> {

	public static final ExecutorService es = AutExecutors.newSingleExecutor();

	protected static final Logger logger = LoggerFactory.getLogger(CompileTask.class);

	protected static final String TARGET_TAG = "Target = ";

	protected ExternalLogger externalLogger;

	protected String makeCleanError;
	protected String makeError;

	protected final IBuildConfig buildConfig;

	protected final String taskId;
	protected final int taskTotal;

	public CompileTask(IBuildConfig buildConfig, int taskTotal) {
		this.buildConfig = buildConfig;
		this.taskId = UUID.randomUUID().toString();
		this.taskTotal = taskTotal;
	}

	protected void makeClean(File directory) throws MakeCleanFailureException, IOException {
		String makeCleanCmd = buildConfig.getMakeClean();
		Process makeCleanProcess = Runtime.getRuntime().exec(makeCleanCmd, null, directory);

		ProcessHandler makeCleanHandler = new ProcessHandler()
				.setProcess(makeCleanProcess)
				.setOnOutput(new OnResponseListener() {
					@Override
					public void receive(String line) throws IOException {
						if (line.startsWith(TARGET_TAG)) {

						} else {
							ExternalLogger.log(externalLogger, line);
						}
					}
				})
				.setOnError(new OnResponseListener() {
					@Override
					public void receive(String line) throws IOException {
						if (makeCleanError == null)
							makeCleanError = line;
						else
							makeCleanError += SpecialCharacter.LINE_BREAK + line;

						ExternalLogger.log(externalLogger, line);
					}
				})
				.setOnException(new OnExceptionListener() {
					@Override
					public void onThrow(Exception e) {
						if (makeCleanError == null)
							makeCleanError = e.getMessage();
						else
							makeCleanError += SpecialCharacter.LINE_BREAK + e.getMessage();
					}
				});

		makeCleanHandler.run();

		if (makeCleanError != null) {
			ExternalLogger.error(externalLogger, "Failed to run make clean");
			throw new MakeCleanFailureException(makeCleanError);
		} else {
			ExternalLogger.info(externalLogger, "Make clean successfully");
			logger.debug("Make clean successfully");
		}
	}

	protected String make(File directory, String... options) throws Exception {
		// generate make command
		StringBuilder makeCmdBuilder = new StringBuilder()
				.append(buildConfig.getMake());
		for (String option : options) {
			makeCmdBuilder.append(" ").append(option);
		}
		final String makeCmd = makeCmdBuilder.toString();

		logger.debug("Run " + makeCmd + " in " + directory);
		Process makeProcess = Runtime.getRuntime().exec(makeCmd, null, directory);

		AtomicReference<String> exePathRef = new AtomicReference<>();

		ProcessHandler makeHandler = new ProcessHandler()
				.setProcess(makeProcess)
				.setOnError(new OnResponseListener() {
					@Override
					public void receive(String line) throws IOException {
						if (line.contains(" error:") || line.contains(" Error:")) {
							if (makeError == null)
								makeError = line;
							else
								makeError += SpecialCharacter.LINE_BREAK + line;

							logger.error("Make command throws an error message");
						}

						ExternalLogger.log(externalLogger, line);
						System.out.println(line);
					}
				})
				.setOnException(new OnExceptionListener() {
					@Override
					public void onThrow(Exception e) {
						if (makeError == null)
							makeError = e.getMessage();
						else
							makeError += SpecialCharacter.LINE_BREAK + e.getMessage();

						logger.error("Make command throws an exception", e);
					}
				})
				.setOnOutput(new OnResponseListener() {
					@Override
					public void receive(String line) throws IOException {
						logger.debug("Make command is processing");
						if (line.startsWith(TARGET_TAG)) {
							String exePath = line.substring(TARGET_TAG.length());
							exePathRef.set(exePath);
						} else {
							ExternalLogger.log(externalLogger, line);
							System.out.println(line);
						}
					}
				});

		es.submit(makeHandler).get();

		if (makeError != null) {
			ExternalLogger.error(externalLogger, "Failed to run make");
			throw new MakeFailureException(makeError);
		} else {
			ExternalLogger.info(externalLogger, "Make project successfully");
			logger.debug("Make project successfully");
		}

		return exePathRef.get();
	}

	public void setExternalLogger(ExternalLogger externalLogger) {
		this.externalLogger = externalLogger;
	}
}
