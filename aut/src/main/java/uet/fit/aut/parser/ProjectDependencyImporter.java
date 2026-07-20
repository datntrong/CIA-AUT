package uet.fit.aut.parser;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.logger.ExternalLogger;
import uet.fit.aut.logger.IdMapping;
import uet.fit.aut.parser.dependency.CompoundDependency;
import uet.fit.aut.parser.dependency.Dependency;
import uet.fit.aut.parser.dependency.IncludeHeaderDependency;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.parser.obj.ISourcecodeFileNode;
import uet.fit.aut.parser.obj.ProjectNode;
import uet.fit.aut.search.Search;
import uet.fit.aut.search.condition.SourcecodeFileNodeCondition;
import uet.fit.aut.util.PathUtils;
import uet.fit.aut.util.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class ProjectDependencyImporter implements ISourceDependencyStorage {

	private static final Logger logger = LoggerFactory.getLogger(ProjectDependencyImporter.class);

	private static final ExecutorService es = ProjectParser.es;

	private @NotNull final ProjectNode root;

	private final Map<String, INode> cache = new ConcurrentHashMap<>();

	private final String taskId;

	private ExternalLogger externalLogger;

	public ProjectDependencyImporter(@NotNull ProjectNode root) {
		this.taskId = UUID.randomUUID().toString();
		this.root = root;
	}

	public void load(@NotNull File dependenciesFile) throws InterruptedException, FileNotFoundException {
		ExternalLogger.log(externalLogger, "Loading dependencies...");

		if (!dependenciesFile.exists())
			throw new FileNotFoundException(dependenciesFile + " (No such dependencies file)");

		ExternalLogger.progress(externalLogger, taskId, "Load dependencies",0,  -1);

		String json = Utils.readFileContent(dependenciesFile);
		JsonArray jsonArray = JsonParser.parseString(json).getAsJsonArray();

		final int taskNum = jsonArray.size();

		ExternalLogger.registerTask(externalLogger, taskId, "Load dependencies", taskNum);

		List<Callable<Void>> handleDependencyTasks = new ArrayList<>();
		AtomicInteger progress = new AtomicInteger();

		for (JsonElement element : jsonArray) {
			final JsonObject jsonDepend = element.getAsJsonObject();

			handleDependencyTasks.add(() -> {
				try {
					handleDependency(jsonDepend);
				} catch (Exception e) {
					logger.error("Cant add dependency", e);
					ExternalLogger.error(externalLogger, e.getMessage());
				}
				ExternalLogger.progress(externalLogger, taskId, "Load dependencies", progress.incrementAndGet(), taskNum);
				return null;
			});
		}

		try {
			es.invokeAll(handleDependencyTasks);
		} finally {
//			es.shutdown();
			ExternalLogger.progress(externalLogger, taskId, "Load dependencies", taskNum, taskNum);
		}
	}

	private void handleDependency(final JsonObject jsonDepend) throws Exception {
		Dependency d = null;

		String dependType = jsonDepend.get(TYPE_TAG).getAsString();

		// ignore include dependencies
		if (dependType.equals(IncludeHeaderDependency.class.getName()))
			return;

		// ignore compound dependencies
		if (dependType.equals(CompoundDependency.class.getName()))
			return;

		JsonObject jsonStart = jsonDepend.get(START_TAG).getAsJsonObject();
		INode start = findNode(jsonStart);

		if (start != null) {
			JsonObject jsonEnd = jsonDepend.get(END_TAG).getAsJsonObject();
			INode end = findNode(jsonEnd);

			if (end != null) {
				d = addDependency(start, end, dependType);
				logger.debug(String.format("Found [%s] %s -> %s", dependType,
						IdMapping.getInstance().getOrCreate(start.getName()),
						IdMapping.getInstance().getOrCreate(end.getName())
				));
			}
		}

		if (d == null) {
			String msg = String.format("Failed to find %s", dependType);
			throw new Exception(msg);
		}
	}

	private synchronized Dependency addDependency(@NotNull INode start, @NotNull INode end, @NotNull String type)
			throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
			InstantiationException, IllegalAccessException {

		Constructor<?> c = Class.forName(type).getConstructor(INode.class, INode.class);
		Dependency d = (Dependency) c.newInstance(start, end);

		synchronized (start.getDependencies()) {
			if (!start.getDependencies().contains(d)) {
				start.getDependencies().add(d);
			}
		}

		synchronized (end.getDependencies()) {
			if (!end.getDependencies().contains(d)) {
				end.getDependencies().add(d);
			}
		}

		return d;
	}

	@Nullable
	private synchronized INode findNode(@NotNull JsonObject jsonObject) {
		String path = jsonObject.get(PATH_TAG).getAsString();

		// cache hit
		if (cache.containsKey(path))
			return cache.get(path);

		String type = jsonObject.get(TYPE_TAG).getAsString();

		INode searchRoot = root;
		if (jsonObject.has(SOURCE_TAG)) {
			String file = jsonObject.get(SOURCE_TAG).getAsString();
			List<ISourcecodeFileNode> candidates = Search.searchNodes(root, new SourcecodeFileNodeCondition(), file);
			if (!candidates.isEmpty())
				searchRoot = candidates.get(0);
		}

		INode node = findNode(searchRoot, type, path);

		// put to cache
		if (node != null)
			cache.put(path, node);

		return node;
	}

	@Nullable
	private INode findNode(@Nullable INode root, @NotNull String clazz, @NotNull String path) {
		if (root == null)
			return null;

		if (PathUtils.equals(root.getAbsolutePath(), path) && root.getClass().getName().equals(clazz))
			return root;

		for (INode child : root.getChildren()) {
			INode node = findNode(child, clazz, path);
			if (node != null)
				return node;
		}

		return null;
	}

	public void setExternalLogger(ExternalLogger externalLogger) {
		this.externalLogger = externalLogger;
	}
}
