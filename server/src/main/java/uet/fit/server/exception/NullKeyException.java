package uet.fit.server.exception;

import org.jetbrains.annotations.NotNull;
import uet.fit.server.DAO.entity.Entity;

public class NullKeyException extends Exception {

	public NullKeyException(@NotNull Entity entity) {
		super(entity + " has null key");
	}
}
