package uet.fit.aut.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.config.pro.ProDefineNode;
import uet.fit.aut.config.pro.ProDependNode;
import uet.fit.aut.config.pro.ProFileNode;
import uet.fit.aut.config.pro.ProHeaderNode;
import uet.fit.aut.config.pro.ProIncludeNode;
import uet.fit.aut.config.pro.ProSourceNode;
import uet.fit.aut.config.pro.ProVPathNode;
import uet.fit.aut.logger.ExternalLogger;
import uet.fit.aut.logger.IdMapping;
import uet.fit.aut.ter.OnFinishListener;
import uet.fit.aut.ter.OnResponseListener;
import uet.fit.aut.ter.ProcessHandler;
import uet.fit.aut.util.PathUtils;
import uet.fit.aut.util.QTConst;
import uet.fit.aut.util.SpecialCharacter;
import uet.fit.aut.util.Utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import static uet.fit.aut.config.ProFileUtils.getCloneProFile;
import static uet.fit.aut.config.ProFileUtils.getProFileInClonedFolder;
import static uet.fit.aut.thread.task.ExecuteTestTask.INSTRUMENT_NAME;
import static uet.fit.aut.util.QTConst.AUT_BUILD_DIR;
import static uet.fit.aut.util.QTConst.MESSAGE_TAG;
import static uet.fit.aut.util.QTConst.WARNING_TAG;

public class ProConfigAnalyzer extends ProcessHandler {

	private static final Logger logger = LoggerFactory.getLogger(ProConfigAnalyzer.class);

	private static final String INCLUDE_PATH_TAG = "[(INCLUDEPATH)]";
	private static final String DEPEND_PATH_TAG = "[(DEPENDPATH)]";
	private static final String VPATH_TAG = "[(VPATH)]";

	private static final String SOURCE_TAG = "[(SOURCE)]";
	private static final String HEADER_TAG = "[(HEADER)]";
	private static final String DEFINE_TAG = "[(DEFINE)]";

	private static final String QTDIR_TAG = "[(QTDIR)]";
	private static final String DESTDIR_TAG = "[(DESTDIR)]";

	private final ProjectConfig projectConfig = new ProjectConfig();

	private String errorMessage;

	private OnSuccessListener onSuccess;

	private OnErrorListener onError;

	private ExternalLogger externalLogger;

	public ProConfigAnalyzer(IBuildConfig buildConfig, File env, File project, File proFile, String user) throws Exception {
		// set pro file path
		projectConfig.setProPath(proFile.getAbsolutePath());
		projectConfig.setProjectPath(project.getAbsolutePath());

		// copy project if not exist
		File originDirectory = new File(projectConfig.getProjectPath());
		File clonedDirectory = env.toPath().resolve(INSTRUMENT_NAME).toFile();

		String proEnvFilePath = getProFileInClonedFolder(clonedDirectory, project, proFile);

		if (!clonedDirectory.exists()) {
			Utils.copy(originDirectory, clonedDirectory);
			// overwrite to set fixed dest dir
			setFixedDestDir(proEnvFilePath);
		}

		// clone file
		String cloneProFile = getCloneProFile(env, user, project, proFile);

		// sau nay se include proEnvFilePath thay vi copy roi ghi de
		// overwrite to log in cmd
		Utils.copyFileUsingChannel(proEnvFilePath, cloneProFile);
		overwriteProFile(cloneProFile);

		// the script: qmake uuid.pro
		String[] script = new String[] {buildConfig.getQmake(), cloneProFile};

		// create the process
		File workingDirectory = new File(cloneProFile).getParentFile();
		Process process = Runtime.getRuntime().exec(script,null, workingDirectory);

		setProcess(process);

		final File proEnvFileContainer = new File(proEnvFilePath).getParentFile();

		setOnError(new OnResponseListener() {
			@Override
			public void receive(String line) throws IOException {
				handle(line, proEnvFileContainer);
			}
		});

		setOnFinish(new OnFinishListener() {
			@Override
			public void onFinish(String out, String err) throws Exception {
				// on callback
				if (errorMessage != null && onError != null)
					onError.onError(errorMessage);

				if (onSuccess != null)
					onSuccess.onSuccess(projectConfig);
			}
		});
	}

	private void handle(String line, File proEnvFileContainer) {
		if (line.contains(INCLUDE_PATH_TAG)) {
			int index = line.indexOf(INCLUDE_PATH_TAG) + INCLUDE_PATH_TAG.length();
			String path = line.substring(index);
			path = preprocessPath(path, proEnvFileContainer);
			String absolutePath = toAbsolute(path);
			if (absolutePath != null) {
				ProIncludeNode includeNode = new ProIncludeNode(path, absolutePath);
				projectConfig.getIncludes().add(includeNode);
				logger.debug("Found an include path " + IdMapping.getInstance().getOrCreate(path) + " in the project config");
			} else {
				appendError(String.format("Include path %s not found", path));
			}
		} else if (line.contains(DEPEND_PATH_TAG)) {
			int index = line.indexOf(DEPEND_PATH_TAG) + DEPEND_PATH_TAG.length();
			String path = line.substring(index);
			path = preprocessPath(path, proEnvFileContainer);
			String absolutePath = toAbsolute(path);
			if (absolutePath != null) {
				ProDependNode dependNode = new ProDependNode(path, absolutePath);
				projectConfig.getDepends().add(dependNode);
				logger.debug("Found a depend path " + IdMapping.getInstance().getOrCreate(path) + " in the project config");
			} else {
				appendError(String.format("Depend path %s not found", path));
			}
		} else if (line.contains(VPATH_TAG)) {
			int index = line.indexOf(VPATH_TAG) + VPATH_TAG.length();
			String path = line.substring(index);
			path = preprocessPath(path, proEnvFileContainer);
			String absolutePath = toAbsolute(path);
			if (absolutePath != null) {
				ProVPathNode vPathNode = new ProVPathNode(path, absolutePath);
				projectConfig.getvPaths().add(vPathNode);
				logger.debug("Found a vpath " + IdMapping.getInstance().getOrCreate(path) + " in the project config");
			} else {
				appendError(String.format("Vpath %s not found", path));
			}
		} else if (line.contains(SOURCE_TAG)) {
			int index = line.indexOf(SOURCE_TAG) + SOURCE_TAG.length();
			String path = line.substring(index);
			path = preprocessPath(path, proEnvFileContainer);
			String absolutePath = toAbsolute(path);
			if (absolutePath != null) {
				projectConfig.getSources().add(new ProSourceNode(path, absolutePath));
				logger.debug("Found a source file " + IdMapping.getInstance().getOrCreate(path) + " in the project config");
			} else {
				appendError(String.format("Source file %s not found", path));
			}
		} else if (line.contains(HEADER_TAG)) {
			int index = line.indexOf(HEADER_TAG) + HEADER_TAG.length();
			String path = line.substring(index);
			path = preprocessPath(path, proEnvFileContainer);
			String absolutePath = toAbsolute(path);
			if (absolutePath != null) {
				projectConfig.getHeaders().add(new ProHeaderNode(path, absolutePath));
				logger.debug("Found a header file " + IdMapping.getInstance().getOrCreate(path) + " in the project config");
			} else {
				appendError(String.format("Header file %s not found", path));
			}
		} else if (line.contains(QTDIR_TAG)) {
			int index = line.indexOf(QTDIR_TAG) + QTDIR_TAG.length();
			String path = line.substring(index);
			String absolutePath = toAbsolute(path);
			projectConfig.setQtDir(absolutePath);
			logger.debug("Found the Qt library directory " + IdMapping.getInstance().getOrCreate(path));
		} else if (line.contains(DESTDIR_TAG)) {
			int index = line.indexOf(DESTDIR_TAG) + DESTDIR_TAG.length();
			String path = line.substring(index);
			// some project may not define this variable
			if (!path.isEmpty()) {
//				String absolutePath = toAbsolute(path);
				projectConfig.setDestDir(path);
				logger.debug("Found the dest directory " + IdMapping.getInstance().getOrCreate(path) + " in the project config");
			}
		} else if (line.contains(DEFINE_TAG)) {
			int index = line.indexOf(DEFINE_TAG) + DEFINE_TAG.length();
			String define = line.substring(index);
			index = define.indexOf(SpecialCharacter.EQUAL);
			String name, value;
			if (index > 0) {
				name = define.substring(0, index);
				value = define.substring(index + 1);
			} else {
				name = define;
				value = null;
			}
			projectConfig.getDefines().add(new ProDefineNode(name, value));
			logger.debug("Found a define " + name + " in the project config");
		} else {
			if (line.contains(MESSAGE_TAG)) {
				logger.debug("Qmake throws an info message");
			} else if (line.contains(WARNING_TAG)) {
				appendError(line);
				logger.debug("Qmake throws a warning message");
			} else {
				appendError(line);
				logger.debug("Qmake throws an error message");
			}
		}
	}

	public void setExternalLogger(ExternalLogger externalLogger) {
		this.externalLogger = externalLogger;
	}

	private void appendError(String line) {
		if (errorMessage == null)
			errorMessage = line;
		else
			errorMessage += "\n" + line;
	}

	private @NotNull String preprocessPath(@NotNull String path, @NotNull File proEnvFileContainer) {
		if (PathUtils.isAbsolute(path)) {
			if (path.startsWith(proEnvFileContainer.getAbsolutePath())) {
				path = PathUtils.relative(new File(path), proEnvFileContainer);
			}
		}

		return path;
	}

	private @Nullable String toAbsolute(@NotNull String path) {
		// fast convert to absolute path by pro file
		String absolutePath = PathUtils.isAbsolute(path)
				? path : PathUtils.absolute(path, new File(projectConfig.getProPath()));

		if (!new File(absolutePath).exists()) {
			// search file in depend paths & vpaths
			List<File> anchorFolders = new ArrayList<>();
			for (ProFileNode node : projectConfig.getDepends())
				anchorFolders.add(new File(node.getAbsolutePath()));
			for (ProFileNode node : projectConfig.getvPaths())
				anchorFolders.add(new File(node.getAbsolutePath()));

			for (File anchor : anchorFolders) {
				absolutePath = PathUtils.absolute(path, anchor);
				if (new File(absolutePath).exists())
					return absolutePath;
			}

			return null;
		} else {
			return absolutePath;
		}
	}

	public void setOnSuccess(OnSuccessListener onSuccess) {
		this.onSuccess = onSuccess;
	}

	public void setOnError(OnErrorListener onError) {
		this.onError = onError;
	}

	private void setFixedDestDir(String path) throws Exception {
		File file = new File(path);

		if (file.exists()) {
			logger.debug("Overwrite " + file.getName() + " file in environment");
		} else {
			logger.error(file.getName() + " not exist in environment");
			throw new Exception(file.getName() + " not exist in environment");
		}

		FileWriter fw = new FileWriter(file, true);
		BufferedWriter bw = new BufferedWriter(fw);
		PrintWriter pw = new PrintWriter(bw);

		String overwriteContent =
				"\nDESTDIR = $$PWD/" + AUT_BUILD_DIR + "\n" +
				"OBJECTS_DIR = $$DESTDIR/.obj\n" +
				"MOC_DIR = $$DESTDIR/.moc\n" +
				"RCC_DIR = $$DESTDIR/.qrc\n" +
				"UI_DIR = $$DESTDIR/.ui\n";
		pw.println(overwriteContent);

		pw.close();
	}

	private void overwriteProFile(String path) throws IOException {
		File file = new File(path);

		FileWriter fw = new FileWriter(file, true);
		BufferedWriter bw = new BufferedWriter(fw);
		PrintWriter pw = new PrintWriter(bw);

		// include env pro file
		String content = "\n\n\n";
		pw.println(content);

		// ignore sdk version
		pw.println(QTConst.IGNORE_SDK_VERSION + SpecialCharacter.LINE_BREAK);

		// print vpath
		String logVPaths = logList(VPATH_TAG, QTConst.VPATHS);
		logger.debug("Append printing vpath list code: " + logVPaths);
		pw.println(logVPaths);

		// print include path
		String logIncludes = logList(INCLUDE_PATH_TAG, QTConst.INCLUDEPATHS);
		logger.debug("Append printing include path list code: " + logIncludes);
		pw.println(logIncludes);

		// print depend path
		String logDepends = logList(DEPEND_PATH_TAG, QTConst.DEPENDPATHS);
		logger.debug("Append printing depend path list code: " + logDepends);
		pw.println(logDepends);

		// print sources
		String logSources = logList(SOURCE_TAG, QTConst.SOURCE_LIST);
		logger.debug("Append printing source list code: " + logSources);
		pw.println(logSources);

		// print headers
		String logHeaders = logList(HEADER_TAG, QTConst.HEADER_LIST);
		logger.debug("Append printing header list code: " + logHeaders);
		pw.println(logHeaders);

		// print defines
		String logDefines = logList(DEFINE_TAG, QTConst.DEFINE_LIST);
		logger.debug("Append printing define list code: " + logDefines);
		pw.println(logDefines);

		// print qt directory
		String logQtDir = logQtDir();
		logger.debug("Append printing QTDIR code: " + logQtDir.replace(SpecialCharacter.LINE_BREAK, SpecialCharacter.SPACE_STR));
		pw.println(logQtDir);

		// print destination directory
		String logDestDir = logVar(DESTDIR_TAG,  QTConst.DESTDIR);
		logger.debug("Append printing DESTDIR code: " + logDestDir);
		pw.println(logDestDir);

//		// config for build at workspace
//		String originPath = projectConfig.getProjectPath();
//		String originDir = assignVar(ORIGIN_DIR, originPath);
//		pw.println(originDir);
//		String prefixInclude = prefixList(QTConst.INCLUDEPATHS);
//		pw.println(prefixInclude);
//		String prefixDepend = prefixList(QTConst.DEPENDPATHS);
//		pw.println(prefixDepend);
//		String prefixVPath = prefixList(QTConst.VPATHS);
//		pw.println(prefixVPath);
//		String prefixResource = replaceList(QTConst.RESOURCES);
//		pw.println(prefixResource);
//		String additionVpath = appendList(QTConst.VPATHS, ORIGIN_DIR);
//		pw.println(additionVpath);
//		String additionIncludePath = appendList(QTConst.INCLUDEPATHS, ORIGIN_DIR);
//		pw.println(additionIncludePath);
//		String additionDependPath = appendList(QTConst.DEPENDPATHS, ORIGIN_DIR);
//		pw.println(additionDependPath);

		pw.close();
	}

	private String logList(String tag, String listName) {
		return String.format("for (item, %s) { message(%s$$item) }", listName, tag);
	}

	private String logQtDir() {
		String prefix = String.format("%s$$[%s]", QTDIR_TAG, QTConst.QT_INSTALL_PREFIX);

		// macos is in lib folder
		return String.format("macx: { message(%s/lib) }\n", prefix) +
				// linux is in include folder
				String.format("else { message(%s/include) }", prefix);
	}

	private String logVar(String tag, String varName) {
		return String.format("message(%s$$%s)", tag, varName);
	}

	public interface OnSuccessListener {
		void onSuccess(ProjectConfig projectConfig) throws Exception;
	}

	public interface OnErrorListener {
		void onError(String err);
	}
}
