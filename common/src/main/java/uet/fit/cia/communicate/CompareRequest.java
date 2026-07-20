package uet.fit.cia.communicate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompareRequest extends RepositoryRequest {
	private static final long serialVersionUID = -1L;

	private final @NotNull String commitA;
	private final @NotNull String commitB;

	public CompareRequest(@NotNull String username, @NotNull String gitUrl,
			@NotNull String gitUsername, @NotNull String gitPassword,
			@NotNull String commitA, @NotNull String commitB) {
		super(username, gitUrl, gitUsername, gitPassword);
		this.commitA = commitA;
		this.commitB = commitB;
	}

	public final @NotNull String getCommitA() {
		return commitA;
	}

	public final @NotNull String getCommitB() {
		return commitB;
	}

	@Override
	public boolean equals(@Nullable Object object) {
		if (!super.equals(object)) return false;
		final CompareRequest that = (CompareRequest) object;
		return commitA.equals(that.commitA)
				&& commitB.equals(that.commitB);
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + commitA.hashCode();
		result = 31 * result + commitB.hashCode();
		return result;
	}
}
