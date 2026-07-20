package uet.fit.aut.thread.task;

import org.eclipse.core.runtime.CoreException;
import org.jetbrains.annotations.NotNull;
import uet.fit.aut.exception.LoadProjectException;
import uet.fit.aut.parser.CppConfig;
import uet.fit.aut.parser.ProjectParser;
import uet.fit.aut.parser.obj.ConstructorNode;
import uet.fit.aut.parser.obj.Node;
import uet.fit.aut.parser.obj.QtHeaderNode;
import uet.fit.aut.parser.qt.QTFileUtils;
import uet.fit.aut.parser.qt.QTParser;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ParseQTTypeTask extends AbstractAUTTask<Void> {

	@NotNull
	private final CppConfig cppConfig;

	@NotNull
	private final List<QtHeaderNode> qtHeaderNodes;

	@NotNull
	private final Map<String, String> defines;

	@NotNull
	private final List<File> considerFiles;

	@NotNull
	private final List<File> considerFolders;

	@NotNull
	private File qtDir;

	private final String env;

	public ParseQTTypeTask(ProjectParser projectParser, String env) throws CoreException, IOException, InterruptedException, LoadProjectException {
		this.cppConfig = projectParser.getCppConfig();
		this.qtHeaderNodes = projectParser.getQtHeaderNodes();
		this.defines = projectParser.getDefines();
		this.considerFiles = projectParser.getConsiderFiles();
		this.considerFolders = projectParser.getConsiderFolders();
		this.qtDir = projectParser.getQtDir();
		this.env = env;
	}

	@Override
	public Void run() throws Exception {
		Map<String, String> realPredefinedMacros = new HashMap<>(defines);

		realPredefinedMacros.putAll(cppConfig.getMacros());

		List<String> includePaths = considerFolders.stream().map(File::getAbsolutePath).collect(Collectors.toList());
		includePaths.addAll(Arrays.asList(cppConfig.getIncludePaths()));
		String[] projectFileList = considerFiles.stream().map(File::getAbsolutePath).toArray(String[]::new);

		String[] macroCodes = null;
		if (qtHeaderNodes != null) {
			List<String> macroList = qtHeaderNodes.stream().map(Node::getAbsolutePath).collect(Collectors.toList());
			macroCodes = macroList.toArray(String[]::new);
		}

		String[] includePathList = includePaths.toArray(String[]::new);

		String[] total = Stream.concat(Arrays.stream(projectFileList), Arrays.stream(macroCodes)).toArray(String[]::new);

		QTParser qtParser = new QTParser(realPredefinedMacros, includePathList, qtDir, total, qtHeaderNodes);
		List<ConstructorNode> constructorNodes = qtParser.parse();

		//save to file
		QTFileUtils.exportQTTypes(constructorNodes, env);

		return null;
	}

}
