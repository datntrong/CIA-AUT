package uet.fit.cia.communicate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.UUID;

public class IdentifiedRequest implements Serializable {
	private static final long serialVersionUID = -1L;

	private final @NotNull String identifier;

	IdentifiedRequest() {
		this.identifier = UUID.randomUUID().toString();
	}

	IdentifiedRequest(@NotNull String identifier) {
		this.identifier = identifier;
	}

	public static @NotNull IdentifiedRequest of() {
		return new IdentifiedRequest();
	}

	public static @NotNull IdentifiedRequest of(@NotNull String identifier) {
		return new IdentifiedRequest(identifier);
	}

	public final @NotNull String getIdentifier() {
		return identifier;
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return this == object || object != null && object.getClass() == getClass();
	}

	@Override
	public int hashCode() {
		return -1;
	}
}
