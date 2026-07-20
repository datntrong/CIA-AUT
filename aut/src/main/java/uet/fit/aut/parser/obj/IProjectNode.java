package uet.fit.aut.parser.obj;

import java.io.File;

/**
 * Represent the project root
 *
 */
public interface IProjectNode extends INode {

	void setQtDirectory(File qtDir);
	File getQtDirectory();

}

