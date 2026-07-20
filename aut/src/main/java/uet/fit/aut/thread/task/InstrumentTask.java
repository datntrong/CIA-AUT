package uet.fit.aut.thread.task;

import uet.fit.aut.config.ProjectConfig;
import uet.fit.aut.instrument.ProjectClone;
import uet.fit.aut.parser.obj.ISourcecodeFileNode;
import uet.fit.aut.parser.obj.ProjectNode;

import java.util.List;

public class InstrumentTask extends AbstractAUTTask<List<ISourcecodeFileNode>> {

	private final ProjectConfig config;
	private final ProjectNode root;
	private final String environmentPath;
	private final String user;

	public InstrumentTask(String environmentPath, ProjectConfig config, ProjectNode root, String user) {
		this.environmentPath = environmentPath;
		this.config = config;
		this.root = root;
		this.user = user;
	}

	@Override
	public List<ISourcecodeFileNode> run() throws Exception {
		new ProjectClone(environmentPath, config, root, user).cloneEnvironment();
//		List<ProSourceNode> proSourceNodes = config.getSources();
//		List<ISourcecodeFileNode> fileNodes = new ArrayList<>();
//		proSourceNodes.forEach(s -> {
//			ISourcecodeFileNode fileNode = searchSource(s.getAbsolutePath());
//			if (fileNode != null)
//				fileNodes.add(fileNode);
//		});
//		return fileNodes;
		return null;
	}

//	private ISourcecodeFileNode searchSource(String path) {
//		List<ISourcecodeFileNode> searchNodes = Search.searchNodes(root, new SourcecodeFileNodeCondition(), path);
//		if (!searchNodes.isEmpty())
//			return searchNodes.get(0);
//		return null;
//	}
}
