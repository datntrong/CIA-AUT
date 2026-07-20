package uet.fit.cia.communicate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Arrays;

public class StringsResponse implements Serializable {
	private static final long serialVersionUID = -1L;

	private final @NotNull String @NotNull [] strings;

	StringsResponse(@NotNull String @NotNull [] strings) {
		this.strings = strings;
	}

	public static @NotNull StringsResponse of(@NotNull String @NotNull [] strings) {
		return new StringsResponse(strings);
	}

	public final @NotNull String @NotNull [] getStrings() {
		return strings;
	}

	@Override
	public boolean equals(@Nullable Object object) {
		if (this == object) return true;
		if (object == null || getClass() != object.getClass()) return false;
		final StringsResponse that = (StringsResponse) object;
		return Arrays.equals(strings, that.strings);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(strings);
	}
}
