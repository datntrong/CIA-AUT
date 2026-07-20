package uet.fit.server.exception;

import uet.fit.aut.testdata.object.IDataNode;

public class DataNodeNotFoundException extends Exception {

	public DataNodeNotFoundException(IDataNode root, String path) {
		super("Can't find " + path + " from " + root.getPathFromRoot());
	}
}
