package uet.fit.server.exception;

public class MultipleOnlineUserException extends Exception {

	public MultipleOnlineUserException(String user) {
		super("User " + user + " has already logon. Please choose another account");
	}
}
