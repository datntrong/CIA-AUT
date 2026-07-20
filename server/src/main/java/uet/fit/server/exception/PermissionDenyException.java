package uet.fit.server.exception;

public class PermissionDenyException extends Exception {
	public PermissionDenyException(String user, String owner) {
		super("User " + user + " try to modify a resource belong to " + owner);
	}
}
