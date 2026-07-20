package uet.fit.cia.cpp;

import io.gitlab.multicia.cpp.CppBuilder;
import io.gitlab.multicia.cpp.CppBuilderException;
import io.gitlab.multicia.cpp.CppProvider;
import io.gitlab.multicia.fs.FileSystemProvider;
import io.gitlab.multicia.graph.Graph;
import io.gitlab.multicia.graph.GraphException;
import io.gitlab.multicia.graph.GraphHelper;
import io.gitlab.multicia.graph.Provider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.cia.cpp.display.ModificationsResponse;
import uet.fit.cia.cpp.display.ProjectResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;

public final class CppApi {
	private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(CppApi.class);

	private static final @NotNull List<Provider> CPP_PROVIDERS
			= List.of(CppProvider.INSTANCE, FileSystemProvider.INSTANCE);

	// TODO: change me whenever there is a change in the tree structure (both in core and in display)
	public static final int VERSION = 16;


	private CppApi() {
	}

//	//region TEST ONLY
//
//	public static void main(@NotNull String[] args) throws GraphException, CppBuilderException, IOException {
//		final long start = System.currentTimeMillis();
//		try {
//			final Path projectPathOld = Path.of("./local/cia/zpaq714");
//			final Path projectJsonOld = projectPathOld.resolve("output.json");
//			final Graph graphOld;
//			if (Files.exists(projectJsonOld)) {
//				graphOld = CppApi.loadCppGraph(projectJsonOld);
//			} else {
//				graphOld = CppApi.buildGppProject(projectPathOld, null, null);
//				CppApi.saveCppGraph(projectJsonOld, graphOld);
//			}
//
//			final Path projectPathNew = Path.of("./local/cia/zpaq715");
//			final Path projectJsonNew = projectPathNew.resolve("output.json");
//			final Graph graphNew;
//			if (Files.exists(projectJsonNew)) {
//				graphNew = CppApi.loadCppGraph(projectJsonNew);
//			} else {
//				graphNew = CppApi.buildGppProject(projectPathNew, null, null);
//				CppApi.saveCppGraph(projectJsonNew, graphNew);
//			}
//
//			final ProjectResponse json = CppGraphDiffer.compare(graphOld, graphNew).buildProject();
//
//			final String toJson = new Gson().toJson(json);
//			Files.writeString(Path.of("display.json"), toJson);
//
//			final ProjectResponse displayJson = new Gson().fromJson(toJson, ProjectResponse.class);
//			System.out.println(displayJson);
//
//		} finally {
//			System.out.println((System.currentTimeMillis() - start) + " milliseconds");
//		}
//	}
//
//	//endregion TEST ONLY


	public static @NotNull Graph loadCppGraph(@NotNull String jsonString) throws GraphException {
		return GraphHelper.graphFromString(jsonString, CPP_PROVIDERS);
	}

	/**
	 * This method will automatically close the input reader, even when an exception is thrown.
	 */
	public static @NotNull Graph loadCppGraph(@NotNull Reader inputReader) throws GraphException {
		return GraphHelper.graphFromReader(inputReader, CPP_PROVIDERS);
	}

	/**
	 * This method will automatically close the input stream, even when an exception is thrown.
	 */
	public static @NotNull Graph loadCppGraph(@NotNull InputStream inputStream) throws GraphException {
		return GraphHelper.graphFromInputStream(inputStream, CPP_PROVIDERS);
	}

	public static @NotNull Graph loadCppGraph(@NotNull Path inputFile) throws GraphException {
		return GraphHelper.graphFromFile(inputFile, CPP_PROVIDERS);
	}


	public static @NotNull String exportCppGraph(@NotNull Graph graph) {
		return GraphHelper.graphToString(graph);
	}

	public static void exportCppGraph(@NotNull Appendable appendable, @NotNull Graph graph) throws IOException {
		GraphHelper.graphToAppendable(appendable, graph);
	}


	/**
	 * This method will automatically close the output writer, even when an exception is thrown.
	 */
	public static void saveCppGraph(@NotNull Writer outputWriter, @NotNull Graph graph) throws IOException {
		GraphHelper.graphToWriter(outputWriter, graph);
	}

	/**
	 * This method will automatically close the output stream, even when an exception is thrown.
	 */
	public static void saveCppGraph(@NotNull OutputStream outputStream, @NotNull Graph graph) throws IOException {
		GraphHelper.graphToOutputStream(outputStream, graph);
	}

	public static void saveCppGraph(@NotNull Path outputFile, @NotNull Graph graph) throws IOException {
		GraphHelper.graphToFile(outputFile, graph);
	}


	public static @NotNull Graph buildQtProject(@NotNull Path projectPath, @NotNull Path projectFile,
			@Nullable Path gppPath, @Nullable Path qmakePath, @Nullable Path makePath, int jobsCount)
			throws IOException, CppBuilderException {
		LOGGER.info("Start CppApi.buildQtProject");
		try {
			final Path projectRealPath = projectPath.toRealPath(LinkOption.NOFOLLOW_LINKS);
			final Path projectRealFile = projectRealPath.resolve(projectFile).toRealPath(LinkOption.NOFOLLOW_LINKS);
			final Path gppRealPath = gppPath != null ? gppPath.toRealPath(LinkOption.NOFOLLOW_LINKS) : null;
			final Path qmakeRealPath = qmakePath != null ? qmakePath.toRealPath(LinkOption.NOFOLLOW_LINKS) : null;
			final Path makeRealPath = makePath != null ? makePath.toRealPath(LinkOption.NOFOLLOW_LINKS) : null;
			final CppEnvironmentParser parser = CppEnvironmentParser.parseQtProjectFile(projectRealFile,
					gppRealPath, qmakeRealPath, makeRealPath, jobsCount);
			return CppBuilder.build(parser.getPredefinedMacros(), parser.getIncludePaths(),
					projectRealPath, parser.getProjectSources(), parser.getProjectHeaders());
		} catch (final IOException | CppBuilderException exception) {
			LOGGER.error("CppApi.buildQtProject failed with exception!", exception);
			throw exception;
		} finally {
			LOGGER.info("End CppApi.buildQtProject");
		}
	}

	public static @NotNull Graph buildGppProject(@NotNull Path projectFolderPath, @Nullable Path gppPath)
			throws IOException, CppBuilderException {
		LOGGER.info("Start CppApi.buildGppProject");
		try {
			final Path projectRealPath = projectFolderPath.toRealPath(LinkOption.NOFOLLOW_LINKS);
			final Path gppRealPath = gppPath != null ? gppPath.toRealPath(LinkOption.NOFOLLOW_LINKS) : null;
			final CppEnvironmentParser parser = CppEnvironmentParser.parseGppProject(projectRealPath, gppRealPath);
			return CppBuilder.build(parser.getPredefinedMacros(), parser.getIncludePaths(),
					projectRealPath, parser.getProjectSources(), parser.getProjectHeaders());
		} catch (final IOException | CppBuilderException exception) {
			LOGGER.error("CppApi.buildGppProject failed with exception!", exception);
			throw exception;
		} finally {
			LOGGER.info("End CppApi.buildGppProject");
		}
	}


	public static @NotNull ProjectResponse compareCppGraphForProject(@NotNull Graph graphA, @NotNull Graph graphB)
			throws GraphException {
		LOGGER.info("Start CppApi.compareCppGraphForProject");
		try {
			return CppGraphDiffer.compare(graphA, graphB).buildProject();
		} catch (final GraphException exception) {
			LOGGER.error("CppApi.compareCppGraphForProject failed with exception!", exception);
			throw exception;
		} finally {
			LOGGER.info("End CppApi.compareCppGraphForProject");
		}
	}

	public static @NotNull ModificationsResponse compareCppGraphForModifications(@NotNull Graph graphA,
			@NotNull Graph graphB) throws GraphException {
		LOGGER.info("Start CppApi.compareCppGraphForModifications");
		try {
			return CppGraphDiffer.compare(graphA, graphB).buildModifications();
		} catch (final GraphException exception) {
			LOGGER.error("CppApi.compareCppGraphForModifications failed with exception!", exception);
			throw exception;
		} finally {
			LOGGER.info("End CppApi.compareCppGraphForModifications");
		}
	}
}
