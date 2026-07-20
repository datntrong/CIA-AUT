package uet.fit.aut.parser.obj;

import java.io.File;

public class FolderNode extends Node implements IHasFileNode {

	@Override
	public File getFile() {
		return new File(getAbsolutePath());
	}
}
