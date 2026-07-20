package uet.fit.aut.parser;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import uet.fit.aut.logger.ExternalLogger;
import uet.fit.aut.parser.dependency.CompoundDependency;
import uet.fit.aut.parser.dependency.Dependency;
import uet.fit.aut.parser.dependency.IncludeHeaderDependency;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.parser.obj.ISourcecodeFileNode;
import uet.fit.aut.parser.obj.ProjectNode;
import uet.fit.aut.search.Search;
import uet.fit.aut.search.condition.HaveDependencyNodeCondition;
import uet.fit.aut.util.Utils;

import java.io.File;
import java.util.List;
import java.util.UUID;

public class ProjectDependencyExporter implements ISourceDependencyStorage {

	private @NotNull final ProjectNode root;

	private final String taskId;

	private ExternalLogger externalLogger;

	public ProjectDependencyExporter(@NotNull ProjectNode root) {
		this.taskId = UUID.randomUUID().toString();
		this.root = root;
	}

	public void save(@NotNull File... files) {
		ExternalLogger.log(externalLogger, "Saving dependencies...");

		List<INode> allNodes = Search.searchNodes(root, new HaveDependencyNodeCondition());

		final int taskNum = allNodes.size();

		ExternalLogger.registerTask(externalLogger, taskId, "Save dependencies", taskNum);

		int progress = 0;

		JsonArray jsonArray = new JsonArray();
		for (INode node : allNodes) {
			for (Dependency d : node.getDependencies()) {
				if (!(d instanceof IncludeHeaderDependency) // ignore include dependencies
						&& !(d instanceof CompoundDependency)
						&& d.getStartArrow().equals(node)
				) {
					JsonObject jsonDepend = toJson(d);
					jsonArray.add(jsonDepend);
				}
			}
			ExternalLogger.progress(externalLogger, taskId, "Save dependencies", ++progress, taskNum);
		}

		String json = new GsonBuilder()
				.setPrettyPrinting()
				.create()
				.toJson(jsonArray);
		for (File file : files) {
			Utils.writeContentToFile(json, file);
		}

		ExternalLogger.progress(externalLogger, taskId, "Save dependencies", taskNum, taskNum);
	}

	private JsonObject toJson(@NotNull Dependency d) {
		JsonObject jsonDepend = new JsonObject();
		jsonDepend.addProperty(TYPE_TAG, d.getClass().getName());

		JsonObject jsonStart = toJson(d.getStartArrow());
		jsonDepend.add(START_TAG, jsonStart);

		JsonObject jsonEnd = toJson(d.getEndArrow());
		jsonDepend.add(END_TAG, jsonEnd);

		return jsonDepend;
	}

	private JsonObject toJson(@NotNull INode node) {
		JsonObject jsonNode = new JsonObject();
		jsonNode.addProperty(TYPE_TAG, node.getClass().getName());
		jsonNode.addProperty(PATH_TAG, node.getAbsolutePath());
		ISourcecodeFileNode source = Utils.getSourcecodeFile(node);
		if (source != null) {
			jsonNode.addProperty(SOURCE_TAG, source.getAbsolutePath());
		}
		return jsonNode;
	}

	public void setExternalLogger(ExternalLogger externalLogger) {
		this.externalLogger = externalLogger;
	}
}
