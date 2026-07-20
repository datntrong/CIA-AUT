package uet.fit.aut.exception;

public class MakeFailureException extends Exception {
	public MakeFailureException(String msg) {
		super("MakeFailure: " + msg);
	}
}
