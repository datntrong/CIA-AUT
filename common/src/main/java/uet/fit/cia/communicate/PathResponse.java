package uet.fit.cia.communicate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Arrays;

public class PathResponse implements Serializable {
	private static final long serialVersionUID = -1L;

	private final @NotNull String @NotNull [] path;

	PathResponse(@NotNull Path path) {
		this.path = Utils.fromPath(path);
	}

	public static @NotNull PathResponse of(@NotNull Path path) {
		return new PathResponse(path);
	}

	public final @NotNull Path getPath(@NotNull Path rootPath) {
		return Utils.toPath(rootPath, path);
	}

	@Override
	public boolean equals(@Nullable Object object) {
		if (this == object) return true;
		if (object == null || getClass() != object.getClass()) return false;
		final PathResponse that = (PathResponse) object;
		return Arrays.equals(path, that.path);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(path);
	}
}
