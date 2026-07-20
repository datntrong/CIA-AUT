package uet.fit.aut.thread.task;

import uet.fit.aut.config.FolderConfig;
import uet.fit.aut.config.ProjectConfig;
import uet.fit.aut.config.pro.ProSourceNode;
import uet.fit.aut.logger.TimeTracker;
import uet.fit.aut.config.IBuildConfig;
import uet.fit.aut.env.build_result.Execution;
import uet.fit.aut.exception.ExecuteTestException;
import uet.fit.aut.exception.QmakeFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.logger.ExternalLogger;
import uet.fit.aut.parser.IProjectLoader;
import uet.fit.aut.parser.obj.ICommonFunctionNode;
import uet.fit.aut.parser.obj.ISourcecodeFileNode;
import uet.fit.aut.stub_manager.StubManager;
import uet.fit.aut.ter.OnExceptionListener;
import uet.fit.aut.ter.OnFinishListener;
import uet.fit.aut.ter.OnResponseListener;
import uet.fit.aut.ter.OnTimeoutListener;
import uet.fit.aut.ter.ProcessHandler;
import uet.fit.aut.execution.testdriver.TestDriverGeneration;
import uet.fit.aut.execution.testdriver.TestDriverGenerationForCpp;
import uet.fit.aut.testcase.TestCase;
import uet.fit.aut.util.DateTimeUtils;
import uet.fit.aut.util.PathUtils;
import uet.fit.aut.util.QTConst;
import uet.fit.aut.util.SpecialCharacter;
import uet.fit.aut.util.TestPathUtils;
import uet.fit.aut.util.Utils;
import uet.fit.config.Config;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class ExecuteTestTask extends CompileTask<Execution> {

	public static final String DRIVER_NAME = "testdriver.";
	public static final String TEST_PATH_NAME = "testpath.tp";
	public static final String EXEC_TRACE_NAME = "result.trc";
	public static final String INSTRUMENT_NAME = "instrument";
	public static final String LAST_VERSION_NAME = "execverion.dt";
	private static final String QTEST_LOG_FILE = "qttest.log";
	public static final String MOC_DIR = ".moc";
	public static final String OBJ_DIR = ".obj";

	private static final Logger logger = LoggerFactory.getLogger(ExecuteTestTask.class);

	private static final String[] EXE_ARGUMENTS = new String[]{
			"-platform", "offscreen",
			"-nocrashhandler", // Disables the crash handler on Unix platforms.
			"-o", QTEST_LOG_FILE // Output Qt test log to file
	};

	private static final String MAKEFILE = "Makefile";

	protected final ProjectConfig projectConfig;

	private final File proFile;
	private final String environment;
	private final String workspace;
	private final TestCase testCase;

	private String runError;
	protected String qmakeError;

	private String execLog;
	private ExternalLogger generalLogger;

	public ExecuteTestTask(IBuildConfig buildConfig, String environment, String workspace, ProjectConfig projectConfig, TestCase testCase) {
		super(buildConfig, 3);
		this.projectConfig = projectConfig;
		this.environment = environment;
		this.proFile = getInstrumentedProFile(projectConfig, environment);
		this.testCase = testCase;
		this.workspace = workspace;
	}

	public void setGeneralLogger(ExternalLogger generalLogger) {
		this.generalLogger = generalLogger;
	}

	private File getInstrumentedProFile(ProjectConfig projectConfig, String environment) {
		String proPath = projectConfig.getProPath();
		File originProFile = new File(proPath);
		File project = new File(projectConfig.getProjectPath());
		String relativePath = PathUtils.relative(originProFile, project);
		String instrumentPath = environment + File.separator + INSTRUMENT_NAME;
		File instrumentDir = new File(instrumentPath);
		String instrumentedProPath = PathUtils.absolute(relativePath, instrumentDir);
		return new File(instrumentedProPath);
	}

	@Override
	public Execution run() throws Exception {
		long startTime = System.currentTimeMillis();

		try {
			return execute(testCase);
		} catch (Exception e) {
			logger.error("Failed to run " + testCase, e);
			throw e;
		} finally {
			updateStatus(testCase);

			TimeTracker.add("Run " + testCase.getName(), System.currentTimeMillis() - startTime);

			deleteEnvironmentFiles();
		}
	}

	private void deleteEnvironmentFiles() {
		// delete files in env
		String destDir = projectConfig.getDestDir();
		if (destDir == null) {
			destDir = environment + File.separator + INSTRUMENT_NAME
					+ File.separator + QTConst.AUT_BUILD_DIR;
		}

		String testDriverName = DRIVER_NAME + taskId;

		String moc = destDir + File.separator + MOC_DIR + File.separator + testDriverName + QTConst.MOC_EXT;
		logger.debug("Delete " + moc);
		Utils.deleteFileOrFolder(new File(moc));

		String obj = destDir + File.separator + OBJ_DIR + File.separator + testDriverName + QTConst.OUT_EXT;
		logger.debug("Delete " + obj);
		Utils.deleteFileOrFolder(new File(obj));
	}

	private void updateStatus(TestCase testCase) {
		String testPathFile = testCase.getTestPathFile();
		if (testPathFile != null && new File(testPathFile).exists()) {
			String testPath = Utils.readFileContent(testPathFile);
			if (TestPathUtils.isCompleted(testPath, testCase.getName()))
				testCase.setStatus(TestCase.STATUS_SUCCESS);
			else
				testCase.setStatus(TestCase.STATUS_RUNTIME_ERR);
		} else {
			testCase.setStatus(TestCase.STATUS_FAILED);
		}
	}

	private Execution execute(TestCase testCase) throws Exception {
		ExternalLogger.log(generalLogger, "Prepare to execute test case " + testCase.getName());
		logger.debug("Execute " + testCase.getName());
		testCase.setStatus(TestCase.STATUS_EXECUTING);

		// STEP 1: find test case folder
		String testCaseFolderPath = workspace + File.separator + testCase.getId();
		File testCaseFolder = new File(testCaseFolderPath);

		// STEP 2: setup test case path
		String testPathFile = testCaseFolderPath + File.separator + TEST_PATH_NAME;
		testCase.setTestPathFile(testPathFile);
		String resultTraceFile = testCaseFolderPath + File.separator + EXEC_TRACE_NAME;
		testCase.setExecutionResultTrace(resultTraceFile);

		// STEP 3: generate test driver
		File testDriver = generateTestDriver(testCase, testCaseFolderPath);
		ExternalLogger.info(generalLogger, "Generate test driver for " + testCase.getName() + " successfully");

		// STEP 4: generate pro file for test case
		File cloneProFile = getClonePro(proFile);
		ICommonFunctionNode sut = testCase.getFunctionNode();
		ISourcecodeFileNode uut = Utils.getSourcecodeFile(sut);
		generateProFile(proFile, cloneProFile, testDriver, uut.getFile());
		ExternalLogger.log(generalLogger, "Generate pro file to run " + testCase.getName());

		ExternalLogger.registerTask(externalLogger, taskId, "Build Project", taskTotal);

		// STEP 5: qmake
		File workingDirectory = cloneProFile.getParentFile();
		try {
			qmake(cloneProFile, workingDirectory);
			ExternalLogger.progress(externalLogger, taskId, "Build Project", 1, taskTotal);
		} catch (Exception e) {
			ExternalLogger.progress(externalLogger, taskId, "Build Project", taskTotal, taskTotal);
			throw e;
		}

		// STEP 6: modify make file
		String makeFilePath = workingDirectory.getAbsolutePath() + File.separator + MAKEFILE;
		overwriteMakefile(makeFilePath);

		// STEP 7: clean & make
		File exeFile;
		String exePath;
		try {
			exePath = make(workingDirectory);
			exeFile = toExeFile(exePath, workingDirectory);
			exePath = exeFile.getAbsolutePath();
			ExternalLogger.progress(externalLogger, taskId, "Build Project", 2, taskTotal);
		} catch (Exception e) {
			ExternalLogger.progress(externalLogger, taskId, "Build Project", taskTotal, taskTotal);
			throw e;
		}
		testCase.setExecutableFile(exePath);

		// STEP 8: delete test path & result trace
		deleteOldResults(testCaseFolder);

		//STEP 9: delete file pro clone
		Utils.deleteFileOrFolder(cloneProFile);

		// STEP 10: run exe file
		LocalDateTime startTime = LocalDateTime.now();
		LocalDateTime endTime;
		try {
			ExternalLogger.log(generalLogger, "Running test case " + testCase.getName());
			runExecutable(exePath, testCaseFolder);
		} catch (ExecuteTestException e) {
			ExternalLogger.error(generalLogger, "Catch a runtime error when executing " + testCase.getName());
			logger.error("Catch a runtime error when executing " + testCase.getName());
			throw e;
		} finally {
			endTime = LocalDateTime.now();
			ExternalLogger.progress(externalLogger, taskId, "Build Project", taskTotal, taskTotal);
//			Utils.deleteFileOrFolder(exeFile);
			logger.debug("Exe file: " + exeFile);
		}

		// STEP 10: update execution version
		String testCaseVersionPath = testCaseFolderPath + File.separator + LAST_VERSION_NAME;
		String lastModifiedTime = DateTimeUtils.toString(testCase.getLastModifiedTime());
		Utils.writeContentToFile(lastModifiedTime, testCaseVersionPath);

		// STEP 11: return execution result
		Execution execution = new Execution(testCase, exePath);
		execution.setTestDriver(testDriver.getAbsolutePath());
		execution.setTestPath(testPathFile);
		execution.setResultPath(resultTraceFile);
		execution.setStartTime(startTime);
		execution.setEndTime(endTime);
		execution.setLog(execLog);
		return execution;
	}

	private File toExeFile(String exePath, File directory) {
		if (PathUtils.isRelative(exePath)) {
			String absoluteExePath = PathUtils.absolute(exePath, directory);
			if (new File(absoluteExePath).exists()) {
				exePath = absoluteExePath;
			} else {
				File destDir = new File(projectConfig.getDestDir());
				exePath = PathUtils.absolute(exePath, destDir);
			}
		}

		return new File(exePath);
	}

	private void qmake(File cloneProFile, File workingDirectory) throws QmakeFailureException, IOException {
		ProcessHandler qmake = new ProcessHandler();
		Process qmakeProcess = Runtime.getRuntime().exec(
				buildConfig.getQmake() + " " + cloneProFile.getAbsolutePath() +
						" -spec linux-g++ CONFIG+=debug CONFIG+=qml_debug"
				, null, workingDirectory);

		qmake.setProcess(qmakeProcess)
				.setOnError(new OnResponseListener() {
					@Override
					public void receive(String line) {
						if (qmakeError == null)
							qmakeError = line;
						else
							qmakeError += SpecialCharacter.LINE_BREAK + line;

						logger.error("QMAKE FAILED: " + line);

						ExternalLogger.error(externalLogger, line);
					}
				}).setOnException(new OnExceptionListener() {
					@Override
					public void onThrow(Exception e) {
						if (qmakeError == null)
							qmakeError = e.getMessage();
						else
							qmakeError += SpecialCharacter.LINE_BREAK + e.getMessage();

						ExternalLogger.error(externalLogger, e.getMessage());
					}
				});

		qmake.run();

		if (qmakeError != null) {
			ExternalLogger.log(generalLogger, "Failed to run qmake");
			throw new QmakeFailureException(qmakeError);
		} else {
			ExternalLogger.info(generalLogger, "Qmake test case " + testCase.getName() + " successfully");
		}
	}

	private void deleteOldResults(File container) {
		File[] children = container.listFiles();
		if (container.isDirectory() && children != null) {
			for (File child : children) {
				if (!child.getName().equals(LAST_VERSION_NAME))
					Utils.deleteFileOrFolder(child);
			}
		}
	}

	private File generateTestDriver(TestCase testCase, String testCaseFolder) throws Exception {
		String name = DRIVER_NAME + taskId;

		TestDriverGeneration testDriverGen;
		String testDriver;
//		if (uut.getName().endsWith(IProjectLoader.C_FILE_SYMBOL)) {
//			testDriverGen = new TestDriverGenerationForC();
//			testDriver = testCaseFolder + File.separator + name + IProjectLoader.C_FILE_SYMBOL;
//		} else {
		testDriverGen = new TestDriverGenerationForCpp();
		testDriver = testCaseFolder + File.separator + name + IProjectLoader.CPP_FILE_SYMBOL;
//		}

		// generate test driver
		testDriverGen.setTestCase(testCase);
		testDriverGen.setName(name);
		testDriverGen.setEnvironmentPath(environment);
		testDriverGen.setOriginProject(projectConfig.getProjectPath());
		testDriverGen.generate();

		String content = testDriverGen.getTestDriver();

		Utils.writeContentToFile(content, testDriver);

		return new File(testDriver);
	}

	private void runExecutable(String exePath, File testCaseDir) throws ExecuteTestException, IOException,
			ExecutionException, InterruptedException {
		// prepare command to run exe file
		// append execute argument
		String[] command = new String[EXE_ARGUMENTS.length + 1];
		System.arraycopy(EXE_ARGUMENTS, 0, command, 1, EXE_ARGUMENTS.length);
		command[0] = exePath;

		ProcessBuilder processBuilder = new ProcessBuilder()
				.command(command)
				.directory(testCaseDir);

		Map<String, String> runtimeEnv = processBuilder.environment();
		if (Utils.isUnix()) {
			// set XDG_RUNTIME_DIR and change permission
			runtimeEnv.put("XDG_RUNTIME_DIR", testCaseDir.getAbsolutePath());
			Set<PosixFilePermission> perms = new HashSet<>();
			perms.add(PosixFilePermission.OWNER_READ);
			perms.add(PosixFilePermission.OWNER_WRITE);
			perms.add(PosixFilePermission.OWNER_EXECUTE);
			Files.setPosixFilePermissions(testCaseDir.toPath(), perms);

			// TODO custom env variables
			runtimeEnv.put("LD_LIBRARY_PATH", "/usr/local/lib");
		}

		Process exeProcess = processBuilder.start();

		ProcessHandler handler = new ProcessHandler();

		handler.setProcess(exeProcess)
				.setTimeout(Config.getRunTestTimeout(), new OnTimeoutListener() {
					@Override
					public void onTimeout() {
						String msg = "The timeout elapsed before " + testCase.getName() + " extermination";
						logger.error(msg);
						ExternalLogger.log(externalLogger, msg);
					}
				})
				.setOnError(new OnResponseListener() {
					@Override
					public void receive(String line) {
						if (line != null) {
							logger.error("Running executable file throws an error message");
							logger.error(line);
							ExternalLogger.log(externalLogger, "[Stderr] " + line);
							if (runError == null) {
								runError = line;
							} else {
								runError += SpecialCharacter.LINE_BREAK + line;
							}

							appendExecuteLog(line);
						}
					}
				})
				.setOnOutput(new OnResponseListener() {
					@Override
					public void receive(String line) throws IOException {
						appendExecuteLog(line);
					}
				})
				.setOnException(new OnExceptionListener() {
					@Override
					public void onThrow(Exception e) {
						if (runError == null) {
							runError = e.getMessage();
						} else {
							runError += SpecialCharacter.LINE_BREAK + e.getMessage();
						}
					}
				})
				.setOnFinish(new OnFinishListener() {
					@Override
					public void onFinish(String out, String err) throws Exception {
						logger.debug("Executing test case " + testCase.getName() + " is finished");
						ExternalLogger.info(generalLogger, "Executing test case " + testCase.getName() + " is finished");

						if (out != null) {
							logger.debug("[Stdout] " + out);
							ExternalLogger.log(externalLogger, "[Stdout] " + out);
						}
					}
				});

		es.submit(handler).get();

		// check if qt test log file generated
		File qtLogFile = testCaseDir.toPath().resolve(QTEST_LOG_FILE).toFile();
		if (!qtLogFile.exists()) {
			throw new ExecuteTestException(testCase.getName(), runError == null ? "" : runError);
		}
	}

	private void appendExecuteLog(String line) {
		if (execLog == null)
			execLog = line;
		else
			execLog += SpecialCharacter.LINE_BREAK + line;
	}

	private void overwriteMakefile(String path) throws IOException {
		File file = new File(path);

		FileWriter fw = new FileWriter(file, true);
		BufferedWriter bw = new BufferedWriter(fw);
		PrintWriter pw = new PrintWriter(bw);
		String line = String.format("$(info %s${TARGET})", TARGET_TAG);
		pw.println(line);

		line = "CXXFLAGS += --time\n" +
				"LFLAGS += --time";
		pw.println(line);

		pw.close();
	}

	private File getClonePro(File proFile) {
		File parent = proFile.getParentFile();
		String path = parent.getAbsolutePath() + File.separator + testCase.getId() + ".pro";
		return new File(path);
	}

	private void generateProFile(File originProFile, File clonedProFile, File testDriver, File testedFile) {
		String folderConfig = FolderConfig.load().getWorkspace();
		String cloneContent = Utils.readFileContent(originProFile);
		if (StubManager.isCheckStaticNormalFunc()) {
			String optionForStubStatic = "QMAKE_CXXFLAGS += -std=c++11 -g -no-pie -fno-stack-protector -Wall -Wno-unused-function  -Wno-unused-variable -Wno-pmf-conversions\n" +
					"INCLUDEPATH += " + folderConfig + "/cpp-stub/src " + folderConfig + "/cpp-stub/src_linux\n" +
					"QMAKE_LINK += -std=c++11 -g -no-pie -fno-stack-protector -Wall -Wno-unused-function  -Wno-unused-variable -Wno-pmf-conversions -I" + folderConfig + "/cpp-stub/src -I" + folderConfig + "/cpp-stub/src_linux\n";

			cloneContent += optionForStubStatic;
			StubManager.setCheckStaticNormalFunc(false);
		}
		// add test driver
		String testDriverRelativePath = PathUtils.relative(testDriver, clonedProFile);
		cloneContent += SpecialCharacter.LINE_BREAK + QTConst.SOURCE_LIST + " += " + testDriverRelativePath;

		// remove origin file
		String testedFileRelativePath = testedFile.getAbsolutePath();
		PathUtils.relative(testedFile, originProFile);
		for (ProSourceNode source : projectConfig.getSources()) {
			if (PathUtils.equals(source.getAbsolutePath(), testedFile.getAbsolutePath())) {
				testedFileRelativePath = source.getPath();
				break;
			}
		}
		cloneContent += SpecialCharacter.LINE_BREAK + QTConst.SOURCE_LIST + " -= " + testedFileRelativePath;

		// ignore sdk version
		cloneContent += SpecialCharacter.LINE_BREAK + QTConst.IGNORE_SDK_VERSION;

		String testLib = "QT += testlib";
		cloneContent += SpecialCharacter.LINE_BREAK + testLib;

		Utils.writeContentToFile(cloneContent, clonedProFile);
	}
}
