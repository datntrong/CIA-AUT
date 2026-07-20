package uet.fit.aut.exception;

public class MakeCleanFailureException extends Exception {
	public MakeCleanFailureException(String msg) {
		super("MakeCleanFailure: " + msg);
	}
}
