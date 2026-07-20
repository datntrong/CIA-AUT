package uet.fit.aut.exception;

public class QmakeFailureException extends Exception {
	public QmakeFailureException(String msg) {
		super("QmakeFailure: " + msg);
	}
}
