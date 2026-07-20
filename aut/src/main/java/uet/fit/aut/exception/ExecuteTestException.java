package uet.fit.aut.exception;

public class ExecuteTestException extends Exception {
	public ExecuteTestException(String test, String msg) {
		super("Failed to execute " + test + ": " + msg);
	}
}
