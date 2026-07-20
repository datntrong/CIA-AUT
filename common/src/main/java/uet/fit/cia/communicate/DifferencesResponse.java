package uet.fit.cia.communicate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Arrays;

public class DifferencesResponse implements Serializable {
	private static final long serialVersionUID = -1L;

	private final @NotNull DifferenceResponse @NotNull [] differences;

	DifferencesResponse(@NotNull DifferenceResponse @NotNull [] differences) {
		this.differences = differences;
	}

	public static @NotNull DifferencesResponse of(@NotNull DifferenceResponse @NotNull [] differences) {
		return new DifferencesResponse(differences);
	}

	public final @NotNull DifferenceResponse @NotNull [] getDifferences() {
		return differences;
	}

	@Override
	public boolean equals(@Nullable Object object) {
		if (this == object) return true;
		if (object == null || getClass() != object.getClass()) return false;
		final DifferencesResponse that = (DifferencesResponse) object;
		return Arrays.equals(differences, that.differences);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(differences);
	}
}
