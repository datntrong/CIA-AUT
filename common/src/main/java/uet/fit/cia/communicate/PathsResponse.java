package uet.fit.cia.communicate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Arrays;

public class PathsResponse implements Serializable {
	private static final long serialVersionUID = -1L;

	private final @NotNull PathResponse @NotNull [] paths;

	public PathsResponse(@NotNull PathResponse @NotNull [] paths) {
		this.paths = paths;
	}

	public static @NotNull PathsResponse of(@NotNull PathResponse @NotNull [] paths) {
		return new PathsResponse(paths);
	}

	public final @NotNull PathResponse @NotNull [] getPaths() {
		return paths;
	}

	@Override
	public boolean equals(@Nullable Object object) {
		if (this == object) return true;
		if (object == null || getClass() != object.getClass()) return false;
		final PathsResponse that = (PathsResponse) object;
		return Arrays.equals(paths, that.paths);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(paths);
	}
}
