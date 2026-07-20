package uet.fit.server.exception;

public class WrongAuthenticationException extends Exception {

	public WrongAuthenticationException() {
		super("Wrong username or password");
	}
}
