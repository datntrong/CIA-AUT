package uet.fit.aut.exception;

import java.io.File;

public class LoadProjectException extends Exception {
	public LoadProjectException(File project) {
		super("Failed to load project " + project.getAbsolutePath());
	}
}
