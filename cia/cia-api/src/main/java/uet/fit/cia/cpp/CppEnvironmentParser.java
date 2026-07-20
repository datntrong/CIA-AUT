package uet.fit.cia.cpp;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CppEnvironmentParser {
	private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(CppEnvironmentParser.class);

	private final @NotNull Map<String, String> predefinedMacros = new HashMap<>();
	private final @NotNull List<Path> includePaths = new ArrayList<>();
	private final @NotNull List<Path> projectSources = new ArrayList<>();
	private final @NotNull List<Path> projectHeaders = new ArrayList<>();


	private CppEnvironmentParser() {
	}


//	//region TEST ONLY
//
//	public static void main(@NotNull String[] args) throws GraphException, CppBuilderException, IOException {
//		final long start = System.currentTimeMillis();
//		try {
//			final Path projectPathOld = Path.of("./local/cia/commits(1)/291870fa8c2a75f4272e8e875f1b280998e6f3c0/TestApp.pro");
//			final Path projectJsonOld = Path.of("./local/cia/commits(1)/291870fa8c2a75f4272e8e875f1b280998e6f3c0/TestApp.pro.json");
//			final Graph graphOld;
//			if (Files.exists(projectJsonOld)) {
//				graphOld = CppApi.loadCppGraph(projectJsonOld);
//			} else {
//				graphOld = CppApi.buildQtProject(projectPathOld, null, null);
//				CppApi.saveCppGraph(projectJsonOld, graphOld);
//			}
//
//			final Path projectPathNew = Path.of("./local/cia/commits(1)/c2afc554720c57433d71cadb9c347776fc1d75c0/TestApp.pro");
//			final Path projectJsonNew = Path.of("./local/cia/commits(1)/c2afc554720c57433d71cadb9c347776fc1d75c0/TestApp.pro.json");
//			final Graph graphNew;
//			if (Files.exists(projectJsonNew)) {
//				graphNew = CppApi.loadCppGraph(projectJsonNew);
//			} else {
//				graphNew = CppApi.buildQtProject(projectPathNew, null, null);
//				CppApi.saveCppGraph(projectJsonNew, graphNew);
//			}
//
//			final DisplayJson json = CppGraphDiffer.compare(graphOld, graphNew);
//
//			final String toJson = new Gson().toJson(json);
//			Files.writeString(Path.of("display.json"), toJson);
//
//			final DisplayJson displayJson = new Gson().fromJson(toJson, DisplayJson.class);
//			System.out.println(displayJson);
//
//		} finally {
//			System.out.println((System.currentTimeMillis() - start) + " milliseconds");
//		}
//	}
//
//	//endregion TEST ONLY


	public @NotNull Map<String, String> getPredefinedMacros() {
		return Collections.unmodifiableMap(predefinedMacros);
	}

	public @NotNull List<Path> getIncludePaths() {
		return Collections.unmodifiableList(includePaths);
	}

	public @NotNull List<Path> getProjectSources() {
		return Collections.unmodifiableList(projectSources);
	}

	public @NotNull List<Path> getProjectHeaders() {
		return Collections.unmodifiableList(projectHeaders);
	}


	public static @NotNull CppEnvironmentParser parseGppProject(@NotNull Path projectPath, @Nullable Path gppPath)
			throws IOException {
		final CppEnvironmentParser parser = new CppEnvironmentParser();
		LOGGER.info("Start parsing Gpp environment variable...");
		parser.parseGpp(projectPath, gppPath);
		LOGGER.info("Start listing project files...");
		parser.listFiles(projectPath);
		LOGGER.info("Finish parsing environment.");
		dumpInfoToLog(parser);
		return parser;
	}

	public static @NotNull CppEnvironmentParser parseQtProjectFile(@NotNull Path projectFile, @Nullable Path gppPath,
			@Nullable Path qmakePath, @Nullable Path makePath, int jobsCount) throws IOException {
		final CppEnvironmentParser parser = new CppEnvironmentParser();
		LOGGER.info("Start parsing Gpp environment variable...");
		parser.parseGpp(projectFile.getParent(), gppPath);
		LOGGER.info("Start parsing Qt project file...");
		parser.parseQtProject(projectFile, qmakePath, makePath, jobsCount);
		LOGGER.info("Finish parsing environment.");
		dumpInfoToLog(parser);
		return parser;
	}

	private static void dumpInfoToLog(@NotNull CppEnvironmentParser parser) {
		LOGGER.info("predefinedMacros.size() = " + parser.getPredefinedMacros().size());

		final List<Path> includePaths = parser.getIncludePaths();
		LOGGER.info("includePaths.size() = " + includePaths.size());
		for (final Path includePath : includePaths) {
			LOGGER.info("includePaths[] = " + includePath);
		}

		LOGGER.info("projectSources.size() = " + parser.getProjectSources().size());
		LOGGER.info("projectHeaders.size() = " + parser.getProjectHeaders().size());
	}

	private static final @NotNull String[] GPP_COMMAND
			= new String[]{"g++", "-x", "c++", "-std=c++14", "-v", "-E", "-dM", "/dev/null"};

	private static final @NotNull Pattern MACRO_PATTERN
			= Pattern.compile("#define\\s+([a-zA-Z_][a-zA-Z0-9_]+)(?:\\s+(.*?))?\\R");

	private static final @NotNull String INCLUDE_START = "#include <...> search starts here:";
	private static final @NotNull String INCLUDE_END = "End of search list.";
	private static final @NotNull String INCLUDE_FRAMEWORK = " (framework directory)";

	/**
	 * <pre>{@literal
	 * #include <...> search starts here:
	 *   /usr/include/c++/7
	 *   /usr/include/x86_64-linux-gnu/c++/7
	 *   /usr/include/c++/7/backward
	 *   /usr/lib/gcc/x86_64-linux-gnu/7/include
	 *   /usr/local/include
	 *   /usr/lib/gcc/x86_64-linux-gnu/7/include-fixed
	 *   /usr/include/x86_64-linux-gnu
	 *   /usr/include
	 * End of search list.
	 * }</pre>
	 */
	private void parseGpp(@NotNull Path projectPath, @Nullable Path gppPath) throws IOException {
		final String[] gppCommand = gppPath != null ? GPP_COMMAND.clone() : GPP_COMMAND;
		if (gppPath != null) gppCommand[0] = gppPath.toRealPath(LinkOption.NOFOLLOW_LINKS).toString();

		LOGGER.info("[g++] Running g++ command at " + projectPath + " ...");
		LOGGER.info("[g++/command] " + String.join(" ", gppCommand));

		final StringBuilder macroString = new StringBuilder();
		final StringBuilder debugString = new StringBuilder();

		try {
			final int exitCode = runWait(
					projectPath,
					gppCommand,
					message -> {
						LOGGER.info("[g++/stdout] " + message);
						macroString.append(message).append('\n');
					},
					message -> {
						LOGGER.info("[g++/stderr] " + message);
						debugString.append(message).append('\n');
					},
					() -> LOGGER.info("[g++] make is still running..."),
					Integer.MAX_VALUE, // no time limit
					Integer.MAX_VALUE // no time limit
			);
			LOGGER.info("[g++] g++ command at " + projectPath + " return " + exitCode);
		} catch (final InterruptedException exception) {
			throw new IOException("g++ were interrupted!", exception);
		}

		final Matcher macroMatcher = MACRO_PATTERN.matcher(macroString);
		while (macroMatcher.find()) {
			final String macroName = macroMatcher.group(1);
			final String macroContent = macroMatcher.group(2);
			predefinedMacros.put(macroName, macroContent.isEmpty() ? "1" : macroContent);
		}

		final int startLength = INCLUDE_START.length();
		final int start = debugString.indexOf(INCLUDE_START) + startLength;
		if (start >= startLength) {
			final int end = debugString.indexOf(INCLUDE_END, start);
			if (end >= start) {
				final String[] includes = debugString.substring(start, end).trim().split("[\r\n]+[ \t]+");
				for (final String include : includes) {
					final String includePath = include.endsWith(INCLUDE_FRAMEWORK)
							? include.substring(0, include.length() - INCLUDE_FRAMEWORK.length())
							: include;
					includePaths.add(Path.of(includePath));
				}
				return;
			}
		}
		throw new IOException("g++ return unexpected result!");
	}


	private void listFiles(@NotNull Path projectPath) throws IOException {
		Files.walkFileTree(projectPath.toRealPath(LinkOption.NOFOLLOW_LINKS), EnumSet.of(FileVisitOption.FOLLOW_LINKS),
				Integer.MAX_VALUE, new FileVisitor<>() {
					private final @NotNull Set<Path> visitedPath = new HashSet<>();

					@Override
					public @NotNull FileVisitResult preVisitDirectory(@NotNull Path path, @NotNull BasicFileAttributes attrs)
							throws IOException {
						final Path realPath = path.toRealPath(LinkOption.NOFOLLOW_LINKS);
						return visitedPath.add(realPath) ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SUBTREE;
					}

					@Override
					public @NotNull FileVisitResult visitFile(@NotNull Path path, @NotNull BasicFileAttributes attrs)
							throws IOException {
						if (visitedPath.add(path.toRealPath())) {
							final Path realPath = path.toRealPath(LinkOption.NOFOLLOW_LINKS);
							final String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
							if (fileName.endsWith(".c") || fileName.endsWith(".cc") || fileName.endsWith(".cpp")
									|| fileName.endsWith(".cxx") || fileName.endsWith(".c++")) {
								projectSources.add(realPath);
							} else if (fileName.endsWith(".h") || fileName.endsWith(".hh") || fileName.endsWith(".hpp")
									|| fileName.endsWith(".hxx") || fileName.endsWith(".h++")) {
								projectHeaders.add(realPath);
							}
						}
						return FileVisitResult.CONTINUE;
					}

					@Override
					public @NotNull FileVisitResult visitFileFailed(@NotNull Path path, @Nullable IOException exception)
							throws IOException {
						if (exception != null) {
							LOGGER.info("Exception while visiting file for listing project files!", exception);
						}
						visitedPath.add(path.toRealPath());
						return FileVisitResult.CONTINUE;
					}

					@Override
					public @NotNull FileVisitResult postVisitDirectory(@NotNull Path path, @Nullable IOException exception)
							throws IOException {
						if (exception != null) {
							LOGGER.info("Exception while visiting directory for listing project files!", exception);
						}
						visitedPath.add(path.toRealPath());
						return FileVisitResult.CONTINUE;
					}
				});
	}

	private static final @NotNull Pattern SOURCES_PATTERN
			= Pattern.compile("\\Q[[SOURCES]\\E(.*?)\\Q[SOURCES]]\\E");
	private static final @NotNull Pattern HEADERS_PATTERN
			= Pattern.compile("\\Q[[HEADERS]\\E(.*?)\\Q[HEADERS]]\\E");
	private static final @NotNull Pattern DEFINES_PATTERN
			= Pattern.compile("\\Q[[DEFINES]\\E(.*?)\\Q[DEFINES]]\\E");
	private static final @NotNull Pattern QT_INSTALL_HEADERS_PATTERN
			= Pattern.compile("\\Q[[QT_INSTALL_HEADERS]\\E(.*?)\\Q[QT_INSTALL_HEADERS]]\\E");
	private static final @NotNull Pattern INCLUDE_PATH_PATTERN
			= Pattern.compile("\\Q[[INCLUDE_PATH]\\E(.*?)\\Q[INCLUDE_PATH]]\\E");

	/**
	 * <pre>{@literal
	 * Project MESSAGE: [[SOURCES]action/actionprovider.cpp[SOURCES]]
	 * Project MESSAGE: [[SOURCES]commonstore.cpp[SOURCES]]
	 * Project MESSAGE: [[SOURCES]main.cpp[SOURCES]]
	 * Project MESSAGE: [[SOURCES]statemachine/TestApp.cpp[SOURCES]]
	 * Project MESSAGE: [[HEADERS]action/actionprovider.h[HEADERS]]
	 * Project MESSAGE: [[HEADERS]action/actiontype.h[HEADERS]]
	 * Project MESSAGE: [[HEADERS]commonstore.h[HEADERS]]
	 * Project MESSAGE: [[HEADERS]flux/action.h[HEADERS]]
	 * Project MESSAGE: [[HEADERS]flux/dispatcher.h[HEADERS]]
	 * Project MESSAGE: [[HEADERS]flux/middleware.h[HEADERS]]
	 * Project MESSAGE: [[HEADERS]flux/store.h[HEADERS]]
	 * Project MESSAGE: [[HEADERS]statemachine/StatemachineInterface.h[HEADERS]]
	 * Project MESSAGE: [[HEADERS]statemachine/TestApp.h[HEADERS]]
	 * Project MESSAGE: [[HEADERS]statemachine/sc_types.h[HEADERS]]
	 * Project MESSAGE: [[DEFINES]QT_DEPRECATED_WARNINGS[DEFINES]]
	 * Project MESSAGE: [[QT_INSTALL_HEADERS]/usr/include/x86_64-linux-gnu/qt5[QT_INSTALL_HEADERS]]
	 * }</pre>
	 */
	private void parseQtProject(@NotNull Path projectFilePath, @Nullable Path qmakePath,
			@Nullable Path makePath, int jobsCount) throws IOException {
		final Path projectFile = projectFilePath.toRealPath(LinkOption.NOFOLLOW_LINKS);
		final Path projectDirectory = projectFile.getParent();
		qtProjectRunQmake(projectFile, projectDirectory, qmakePath);
		qtProjectRunMake(projectDirectory, makePath, jobsCount);
	}

	private static void qtProjectRunMake(@NotNull Path projectDirectory, @Nullable Path makePath, int jobsCount)
			throws IOException {
		final String makeString = makePath != null
				? makePath.toRealPath(LinkOption.NOFOLLOW_LINKS).toString()
				: "make";
		final String[] makeCommand = new String[]{makeString, "-j" + Math.max(jobsCount, 1)};

		LOGGER.info("[make] Running make command at " + projectDirectory + " ...");
		LOGGER.info("[make/command] " + String.join(" ", makeCommand));

		try {
			final int exitCode = runWait(
					projectDirectory,
					makeCommand,
					message -> LOGGER.info("[make/stdout] " + message),
					message -> LOGGER.info("[make/stderr] " + message),
					() -> LOGGER.info("[make] make is still running..."),
					Integer.MAX_VALUE, // no time limit
					Integer.MAX_VALUE // no time limit
			);
			LOGGER.info("[make] Make command at " + projectDirectory + " return " + exitCode);
		} catch (final InterruptedException exception) {
			throw new IOException("Make were interrupted!", exception);
		}
	}

	private void qtProjectRunQmake(@NotNull Path projectFile, @NotNull Path projectDirectory, @Nullable Path qmakePath)
			throws IOException {
		final String projectFileName = projectFile.getFileName().toString();
		final Path tempFile = projectDirectory.resolve(projectFileName + "." + UUID.randomUUID() + ".pro");

		try (final PrintWriter writer = new PrintWriter(Files.newBufferedWriter(tempFile))) {
			writer.println(Files.readString(projectFile));
			writer.println("for (item, SOURCES) { message([[SOURCES]$$item[SOURCES]]) }");
			writer.println("for (item, HEADERS) { message([[HEADERS]$$item[HEADERS]]) }");
			writer.println("for (item, DEFINES) { message([[DEFINES]$$item[DEFINES]]) }");
			writer.println("message([[QT_INSTALL_HEADERS]$$[QT_INSTALL_HEADERS][QT_INSTALL_HEADERS]])");
			writer.println("for (item, INCLUDEPATH) { message([[INCLUDE_PATH]$$item[INCLUDE_PATH]]) }");
		} catch (final IOException exception) {
			LOGGER.error("Failed to write to temporary .pro file at " + tempFile, exception);
			throw exception;
		}

		final String qmakeString = qmakePath != null
				? qmakePath.toRealPath(LinkOption.NOFOLLOW_LINKS).toString()
				: "qmake";
		final String[] qmakeCommand = {qmakeString, tempFile.toString()};

		LOGGER.info("[qmake] Running qmake command at " + projectDirectory + " ...");
		LOGGER.info("[qmake/command] " + String.join(" ", qmakeCommand));

		final StringBuilder messages = new StringBuilder();

		try {
			final int exitCode = runWait(
					projectDirectory,
					qmakeCommand,
					message -> LOGGER.info("[qmake/stdout] " + message),
					message -> {
						LOGGER.info("[qmake/stderr] " + message);
						messages.append(message).append('\n');
					},
					() -> LOGGER.info("[qmake] qmake is still running..."),
					Integer.MAX_VALUE, // no time limit
					Integer.MAX_VALUE // no time limit
			);
			LOGGER.info("[qmake] Qmake command at " + projectDirectory + " return " + exitCode);
		} catch (final InterruptedException exception) {
			throw new IOException("Qmake were interrupted!", exception);
		}

		final Matcher sourceMatcher = SOURCES_PATTERN.matcher(messages);
		while (sourceMatcher.find()) {
			projectSources.add(projectDirectory.resolve(sourceMatcher.group(1)));
		}

		final Matcher headerMatcher = HEADERS_PATTERN.matcher(messages);
		while (headerMatcher.find()) {
			projectHeaders.add(projectDirectory.resolve(headerMatcher.group(1)));
		}

		final Matcher defineMatcher = DEFINES_PATTERN.matcher(messages);
		while (defineMatcher.find()) {
			predefinedMacros.put(defineMatcher.group(1), "1");
		}

		final Matcher qtInstallHeadersMatcher = QT_INSTALL_HEADERS_PATTERN.matcher(messages);
		while (qtInstallHeadersMatcher.find()) {
			final Path qtInstallHeadersPath = Path.of(qtInstallHeadersMatcher.group(1));
			includePaths.add(qtInstallHeadersPath);
			try (final DirectoryStream<Path> qtInstallHeaders = Files.newDirectoryStream(qtInstallHeadersPath)) {
				for (final Path qtInstallHeader : qtInstallHeaders) {
					if (Files.isDirectory(qtInstallHeader)) {
						includePaths.add(qtInstallHeader);
					}
				}
			}
		}

		final Matcher includePathMatcher = INCLUDE_PATH_PATTERN.matcher(messages);
		while (includePathMatcher.find()) {
			includePaths.add(Path.of(includePathMatcher.group(1)));
		}
	}

	private static int runWait(@NotNull Path workingDir, @NotNull String @NotNull [] commands,
			@NotNull Consumer<String> inputLineConsumer, @NotNull Consumer<String> errorLineConsumer,
			@NotNull Runnable runningCallback, long runTimeLimit, long terminateTimeLimit)
			throws IOException, InterruptedException {
		final Process process = new ProcessBuilder(commands).directory(workingDir.toFile()).start();
		process.getOutputStream().close();
		try (final Reader inputReader = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8);
				final Reader errorReader = new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8)) {
			final char[] buffer = new char[0x1000];
			final StringBuilder inputBuilder = new StringBuilder();
			final StringBuilder errorBuilder = new StringBuilder();
			boolean inputSkipLF = false, errorSkipLF = false;
			boolean canDestroy = true;
			long previousTime = System.currentTimeMillis(), waitingTime = 0, runningTime = 0;
			while (inputReader.ready() || errorReader.ready() || process.isAlive()) {
				if (inputReader.ready()) {
					final int readCount = inputReader.read(buffer);
					if (readCount > 0) {
						waitingTime = 0;
						int index = inputSkipLF && buffer[0] == '\n' ? 1 : 0;
						while (index < readCount) {
							final char c = buffer[index++];
							if (c != '\r' && c != '\n') {
								inputBuilder.append(c);
							} else {
								inputLineConsumer.accept(inputBuilder.toString());
								inputBuilder.setLength(0);
								inputSkipLF = index == readCount && c == '\r';
							}
						}
					}
				}
				if (errorReader.ready()) {
					final int readCount = errorReader.read(buffer);
					if (readCount > 0) {
						waitingTime = 0;
						int index = errorSkipLF && buffer[0] == '\n' ? 1 : 0;
						while (index < readCount) {
							final char c = buffer[index++];
							if (c != '\r' && c != '\n') {
								errorBuilder.append(c);
							} else {
								errorLineConsumer.accept(errorBuilder.toString());
								errorBuilder.setLength(0);
								errorSkipLF = index == readCount && c == '\r';
							}
						}
					}
				}
				while (process.isAlive() && !inputReader.ready() && !errorReader.ready()) {
					final long currentTime = System.currentTimeMillis();
					final long delta = currentTime - previousTime;
					previousTime = currentTime;
					runningTime += delta;
					if (canDestroy) {
						if (runningTime >= runTimeLimit) {
							process.destroy();
							canDestroy = false;
							runningTime -= runTimeLimit;
						}
					} else {
						if (runningTime > terminateTimeLimit) {
							process.destroyForcibly().waitFor();
							return process.exitValue();
						}
					}
					waitingTime += delta;
					if (waitingTime >= 10000L) {
						waitingTime -= 10000L;
						runningCallback.run();
					}
					process.waitFor(100, TimeUnit.MILLISECONDS);
				}
			}
			inputLineConsumer.accept(inputBuilder.toString());
			errorLineConsumer.accept(errorBuilder.toString());
		} catch (final InterruptedException exception) {
			process.destroyForcibly();
			throw exception;
		}
		return process.exitValue();
	}
}