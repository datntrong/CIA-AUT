package uet.fit.server.exception;

import org.jetbrains.annotations.NotNull;

public class InvalidTestDataException extends Exception {

	public InvalidTestDataException(@NotNull String node) {
		super(node + " is not test data root");
	}
}
