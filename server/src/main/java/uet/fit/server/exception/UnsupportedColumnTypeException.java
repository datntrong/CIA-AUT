package uet.fit.server.exception;

import org.jetbrains.annotations.NotNull;

public class UnsupportedColumnTypeException extends Exception {

	public UnsupportedColumnTypeException(@NotNull Class<?> clazz) {
		super(clazz.getName() + " is not supported for columns");
	}
}
