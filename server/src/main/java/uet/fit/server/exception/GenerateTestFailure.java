package uet.fit.server.exception;

import org.jetbrains.annotations.NotNull;

public class GenerateTestFailure extends Exception {
	public GenerateTestFailure(@NotNull String uut, @NotNull String sut) {
		super("Can't generate test data for " + sut + " in " + uut);
	}
}
