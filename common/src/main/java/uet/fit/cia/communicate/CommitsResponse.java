package uet.fit.cia.communicate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Arrays;

public class CommitsResponse implements Serializable {
	private static final long serialVersionUID = -1L;

	private final @NotNull CommitResponse @NotNull [] commits;

	CommitsResponse(@NotNull CommitResponse @NotNull [] commits) {
		this.commits = commits;
	}

	public static @NotNull CommitsResponse of(@NotNull CommitResponse @NotNull [] commits) {
		return new CommitsResponse(commits);
	}

	public final @NotNull CommitResponse @NotNull [] getCommits() {
		return commits;
	}

	@Override
	public boolean equals(@Nullable Object object) {
		if (this == object) return true;
		if (object == null || getClass() != object.getClass()) return false;
		final CommitsResponse that = (CommitsResponse) object;
		return Arrays.equals(commits, that.commits);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(commits);
	}
}
