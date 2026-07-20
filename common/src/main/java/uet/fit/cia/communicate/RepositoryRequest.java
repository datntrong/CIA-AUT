package uet.fit.cia.communicate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RepositoryRequest extends IdentifiedRequest {
	private static final long serialVersionUID = -1L;

	private final @NotNull String username;
	private final @NotNull String gitUrl;
	private final @NotNull String gitUsername;
	private final @NotNull String gitPassword;

	RepositoryRequest(@NotNull String username, @NotNull String gitUrl,
			@NotNull String gitUsername, @NotNull String gitPassword) {
		this.username = username;
		this.gitUrl = gitUrl;
		this.gitUsername = gitUsername;
		this.gitPassword = gitPassword;
	}

	public static @NotNull RepositoryRequest of(@NotNull String username, @NotNull String gitUrl,
			@NotNull String gitUsername, @NotNull String gitPassword) {
		return new RepositoryRequest(username, gitUrl, gitUsername, gitPassword);
	}

	public final @NotNull String getUsername() {
		return username;
	}

	public final @NotNull String getGitUrl() {
		return gitUrl;
	}

	public final @NotNull String getGitUsername() {
		return gitUsername;
	}

	public final @NotNull String getGitPassword() {
		return gitPassword;
	}

	@Override
	public boolean equals(@Nullable Object object) {
		if (this == object) return true;
		if (object == null || object.getClass() != getClass()) return false;
		final RepositoryRequest that = (RepositoryRequest) object;
		return username.equals(that.username)
				&& gitUrl.equals(that.gitUrl)
				&& gitUsername.equals(that.gitUsername)
				&& gitPassword.equals(that.gitPassword);
	}

	@Override
	public int hashCode() {
		int result = 1;
		result = 31 * result + username.hashCode();
		result = 31 * result + gitUrl.hashCode();
		result = 31 * result + gitUsername.hashCode();
		result = 31 * result + gitPassword.hashCode();
		return result;
	}
}
