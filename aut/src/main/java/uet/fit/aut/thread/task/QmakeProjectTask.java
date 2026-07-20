package uet.fit.aut.thread.task;

import uet.fit.aut.config.IBuildConfig;
import uet.fit.aut.config.ProConfigAnalyzer;
import uet.fit.aut.config.ProjectConfig;
import uet.fit.aut.logger.ExternalLogger;
import uet.fit.aut.ter.OnExceptionListener;
import uet.fit.aut.util.SpecialCharacter;

import java.io.File;
import java.util.UUID;

public class QmakeProjectTask extends AbstractAUTTask<ProjectConfig>{

	private ProjectConfig projectConfig;

	private final File proFile;
	private final File project;
	private final File env;
	private final IBuildConfig buildConfig;
	private final String user;

	protected ExternalLogger externalLogger;
	protected final String taskId;

	protected String qmakeError;

	public QmakeProjectTask(File proFile, File project, File env, IBuildConfig buildConfig, String user) {
		this.proFile = proFile;
		this.project = project;
		this.env = env;
		this.buildConfig = buildConfig;
		this.taskId = UUID.randomUUID().toString();
		this.user = user;
	}

	@Override
	public ProjectConfig run() throws Exception {
		ExternalLogger.progress(externalLogger, taskId, "Build Project", -1, 1);

		ProConfigAnalyzer proConfigAnalyzer = new ProConfigAnalyzer(buildConfig, env, project, proFile, user);
		proConfigAnalyzer.setExternalLogger(externalLogger);
		proConfigAnalyzer.setOnSuccess(new ProConfigAnalyzer.OnSuccessListener() {
			@Override
			public void onSuccess(ProjectConfig proConfig) throws Exception {
				projectConfig = proConfig;
			}
		});

		proConfigAnalyzer.setOnError(new ProConfigAnalyzer.OnErrorListener() {
			@Override
			public void onError(String err) {
				if (qmakeError == null)
					qmakeError = err;
				else
					qmakeError += SpecialCharacter.LINE_BREAK + err;
			}
		});

		proConfigAnalyzer.setOnException(new OnExceptionListener() {
			@Override
			public void onThrow(Exception e) {
				if (qmakeError == null)
					qmakeError = e.getMessage();
				else
					qmakeError += SpecialCharacter.LINE_BREAK + e.getMessage();
			}
		});

		proConfigAnalyzer.run();

		if (qmakeError != null) {
			ExternalLogger.error(externalLogger, "Qmake throws an error");
			ExternalLogger.error(externalLogger, qmakeError);

			// ignore qmake error
		}

		ExternalLogger.progress(externalLogger, taskId, "Build Project", 1, 1);

		ExternalLogger.info(externalLogger, "Run qmake successfully");

		return projectConfig;
	}

	public void setExternalLogger(ExternalLogger externalLogger) {
		this.externalLogger = externalLogger;
	}
}
