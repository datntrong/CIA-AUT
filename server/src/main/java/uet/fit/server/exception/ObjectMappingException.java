package uet.fit.server.exception;

import org.jetbrains.annotations.NotNull;

public class ObjectMappingException extends Exception {

	public ObjectMappingException(@NotNull Class<?> clazz) {
		super(clazz.getName() + " is not representing a table");
	}
}
