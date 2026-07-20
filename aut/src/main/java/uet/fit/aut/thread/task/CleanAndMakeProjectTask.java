package uet.fit.aut.thread.task;

import uet.fit.aut.config.IBuildConfig;
import uet.fit.aut.exception.MakeFailureException;
import uet.fit.aut.logger.ExternalLogger;
import uet.fit.aut.util.QTConst;

import java.io.File;

public class CleanAndMakeProjectTask extends CompileTask<Void> {

	private final File directory;

	public CleanAndMakeProjectTask(File directory, IBuildConfig buildConfig) {
		super(buildConfig, 1);
		this.directory = directory;
	}

	@Override
	public Void run() throws Exception {
		ExternalLogger.info(externalLogger, "Generating binary files...");
		ExternalLogger.progress(externalLogger, taskId, "Generating *.o", -1, taskTotal);
		logger.debug("Generating binary files...");

		try {
			makeClean(directory);
		} catch (Exception e) {
			ExternalLogger.progress(externalLogger, taskId, "Generating *.o", taskTotal, taskTotal);
			throw e;
		}

		try {
			make(directory, QTConst.IGNORE_ERROR_FLAG);
		} catch (MakeFailureException ignored) {

		} finally {
			ExternalLogger.progress(externalLogger, taskId, "Generating *.o", taskTotal, taskTotal);
		}

		ExternalLogger.info(externalLogger, "Binary files generated");
		logger.debug("Binary files generated");

		return null;
	}
}
