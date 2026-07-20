package uet.fit.cia.communicate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

public class DifferenceResponse implements Serializable {
	private static final long serialVersionUID = -1L;

	private final @NotNull String @NotNull [] path;
	private final @NotNull Type type;
	private final @NotNull Change change;

	DifferenceResponse(@NotNull Path path, @NotNull Type type, @NotNull Change change) {
		this.path = Utils.fromPath(path);
		this.type = type;
		this.change = change;
	}

	public static @NotNull DifferenceResponse of(@NotNull Path path, @NotNull Type type, @NotNull Change change) {
		return new DifferenceResponse(path, type, change);
	}

	public final @NotNull Path getPath(@NotNull Path rootPath) {
		return Utils.toPath(rootPath, path);
	}

	public final @NotNull Change getChange() {
		return change;
	}

	@Override
	public boolean equals(@Nullable Object object) {
		if (this == object) return true;
		if (object == null || getClass() != object.getClass()) return false;
		final DifferenceResponse that = (DifferenceResponse) object;
		return type == that.type
				&& change == that.change
				&& Arrays.equals(path, that.path);
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(type, change);
		result = 31 * result + Arrays.hashCode(path);
		return result;
	}

	public enum Type {
		FILE,
		DIRECTORY,
		SYMLINK,
		SUBMODULE
	}

	public enum Change {
		ADDED,
		CHANGED,
		UNCHANGED,
		REMOVED
	}
}
