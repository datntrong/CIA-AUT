package uet.fit.aut.parser.obj;

import java.io.File;

public class ProjectNode extends Node implements IProjectNode, IHasFileNode {

	private File qtDir;

	@Override
	public File getFile() {
		return new File(getAbsolutePath());
	}

	@Override
	public void setQtDirectory(File qtDir) {
		this.qtDir = qtDir;
	}

	@Override
	public File getQtDirectory() {
		return qtDir;
	}
}

