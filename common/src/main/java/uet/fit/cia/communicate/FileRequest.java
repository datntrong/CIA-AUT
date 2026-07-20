package uet.fit.cia.communicate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Arrays;

public class FileRequest extends CommitRequest {
	private static final long serialVersionUID = -1L;

	private final @NotNull String @NotNull [] path;

	FileRequest(@NotNull String username, @NotNull String gitUrl,
			@NotNull String gitUsername, @NotNull String gitPassword,
			@NotNull String commit, @NotNull Path path) {
		super(username, gitUrl, gitUsername, gitPassword, commit);
		this.path = Utils.fromPath(path);
	}

	public static @NotNull FileRequest of(@NotNull String username, @NotNull String gitUrl,
			@NotNull String gitUsername, @NotNull String gitPassword,
			@NotNull String commit, @NotNull Path path) {
		return new FileRequest(username, gitUrl, gitUsername, gitPassword, commit, path);
	}

	public final @NotNull Path getPath(@NotNull Path rootPath) {
		return Utils.toPath(rootPath, path);
	}

	@Override
	public boolean equals(@Nullable Object object) {
		if (!super.equals(object)) return true;
		final FileRequest that = (FileRequest) object;
		return Arrays.equals(path, that.path);
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + Arrays.hashCode(path);
		return result;
	}
}
