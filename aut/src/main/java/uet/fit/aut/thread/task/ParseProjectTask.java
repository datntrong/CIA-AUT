package uet.fit.aut.thread.task;

import org.eclipse.core.runtime.CoreException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uet.fit.aut.config.ProjectConfig;
import uet.fit.aut.config.pro.ProDefineNode;
import uet.fit.aut.config.pro.ProHeaderNode;
import uet.fit.aut.config.pro.ProIncludeNode;
import uet.fit.aut.config.pro.ProSourceNode;
import uet.fit.aut.exception.LoadProjectException;
import uet.fit.aut.logger.ExternalLogger;
import uet.fit.aut.parser.ProjectDependencyExporter;
import uet.fit.aut.parser.ProjectDependencyImporter;
import uet.fit.aut.parser.ProjectParser;
import uet.fit.aut.parser.obj.ProjectNode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParseProjectTask extends AbstractAUTTask<ProjectNode> {

	@NotNull
	private final ProjectConfig projectConfig;

	@NotNull
	private ProjectParser projectParser;

	@Nullable
	private ExternalLogger externalLogger;

	@Nullable
	private File dependenciesFile;

	private boolean loadDependenciesFromFile;

	public ParseProjectTask(@NotNull ProjectConfig projectConfig) {
		this.projectConfig = projectConfig;
	}

	@Override
	public ProjectNode run() throws InterruptedException, LoadProjectException, IOException, CoreException {
		// parse project
		String projectPath = projectConfig.getProjectPath();
		projectParser = new ProjectParser(new File(projectPath));
		projectParser.setExternalLogger(externalLogger);

		// ignore dest dir
		String destDir = projectConfig.getDestDir();
		if (destDir != null) {
			projectParser.setIgnoreFolders(Collections.singletonList(new File(destDir)));
		}

		// search consider files (sources & headers)
		List<File> considerFiles = searchConsiderFiles(projectConfig);
		projectParser.setConsiderFiles(considerFiles);

		projectParser.setQtDir(new File(projectConfig.getQtDir()));

		Map<String, String> defines = new HashMap<>();
		for (ProDefineNode defineNode : projectConfig.getDefines()) {
			String value = defineNode.getValue();
			if (value == null) value = "";
			defines.put(defineNode.getName(), value);
		}
		projectParser.setDefines(defines);

		// search consider folders (include folder)
		List<File> considerFolders = searchConsiderFolders(projectConfig);
		projectParser.setConsiderFolders(considerFolders);

		// don't analyzing dependencies
		if (loadDependenciesFromFile && dependenciesFile != null && dependenciesFile.exists()) {
			projectParser.disableAnalyzeDependencies();
		}

		ProjectNode root = projectParser.getRootTree();

		// save or load dependencies
		if (dependenciesFile != null) {
			if (loadDependenciesFromFile && dependenciesFile.exists()) {
				ProjectDependencyImporter dependencyImporter = new ProjectDependencyImporter(root);
				dependencyImporter.setExternalLogger(externalLogger);
				dependencyImporter.load(dependenciesFile);
			} else {
				ProjectDependencyExporter dependencyExporter = new ProjectDependencyExporter(root);
				dependencyExporter.setExternalLogger(externalLogger);
				dependencyExporter.save(dependenciesFile);
			}
		}

		return root;
	}

	public void setExternalLogger(@NotNull ExternalLogger externalLogger) {
		this.externalLogger = externalLogger;
	}

	private List<File> searchConsiderFiles(@NotNull ProjectConfig projectConfig) {
		List<File> considerFiles = new ArrayList<>();

		for (ProHeaderNode header : projectConfig.getHeaders()) {
			considerFiles.add(new File(header.getAbsolutePath()));
		}

		for (ProSourceNode source : projectConfig.getSources()) {
			considerFiles.add(new File(source.getAbsolutePath()));
		}

		return considerFiles;
	}

	private List<File> searchConsiderFolders(@NotNull ProjectConfig projectConfig) {
		List<File> considerFolders = new ArrayList<>();
		for (ProIncludeNode include : projectConfig.getIncludes()) {
			considerFolders.add(new File(include.getAbsolutePath()));
		}
		return considerFolders;
	}

	public void setLoadDependenciesFromFile(boolean loadDependenciesFromFile) {
		this.loadDependenciesFromFile = loadDependenciesFromFile;
	}

	public void setDependenciesFile(@NotNull File dependenciesFile) {
		this.dependenciesFile = dependenciesFile;
	}

	public ProjectParser getProjectParser() {
		return projectParser;
	}
}
