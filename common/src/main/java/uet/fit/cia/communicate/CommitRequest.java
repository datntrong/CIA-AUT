package uet.fit.cia.communicate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CommitRequest extends RepositoryRequest {
	private static final long serialVersionUID = -1L;

	private final @NotNull String commit;

	CommitRequest(@NotNull String username, @NotNull String gitUrl,
			@NotNull String gitUsername, @NotNull String gitPassword, @NotNull String commit) {
		super(username, gitUrl, gitUsername, gitPassword);
		this.commit = commit;
	}

	public static @NotNull CommitRequest of(@NotNull String username, @NotNull String gitUrl,
			@NotNull String gitUsername, @NotNull String gitPassword, @NotNull String commit) {
		return new CommitRequest(username, gitUrl, gitUsername, gitPassword, commit);
	}

	public final @NotNull String getCommit() {
		return commit;
	}

	@Override
	public boolean equals(@Nullable Object object) {
		if (!super.equals(object)) return false;
		final CommitRequest that = (CommitRequest) object;
		return commit.equals(that.commit);
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + commit.hashCode();
		return result;
	}
}