package uet.fit.server.exception;

import uet.fit.dto.test.ModifyTestDTO;

public class InvalidTestChangeException extends Exception {
	public InvalidTestChangeException(ModifyTestDTO.ChangeType changeType) {
		super("Invalid Change Operation: " + changeType);
	}
}
