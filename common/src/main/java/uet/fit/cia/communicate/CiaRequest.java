package uet.fit.cia.communicate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Arrays;

public class CiaRequest extends CompareRequest {
	private static final long serialVersionUID = -1L;

	private final @NotNull String @NotNull [] proPathA;
	private final @NotNull String @NotNull [] proPathB;
	private final boolean forceReload;

	CiaRequest(@NotNull String username, @NotNull String gitUrl,
			@NotNull String gitUsername, @NotNull String gitPassword,
			@NotNull String commitA, @NotNull String commitB,
			@NotNull Path proPathA, @NotNull Path proPathB, boolean forceReload) {
		super(username, gitUrl, gitUsername, gitPassword, commitA, commitB);
		this.proPathA = Utils.fromPath(proPathA);
		this.proPathB = Utils.fromPath(proPathB);
		this.forceReload = forceReload;
	}

	public static @NotNull CiaRequest of(@NotNull String username, @NotNull String gitUrl,
			@NotNull String gitUsername, @NotNull String gitPassword,
			@NotNull String commitA, @NotNull String commitB,
			@NotNull Path proPathA, @NotNull Path proPathB, boolean forceReload) {
		return new CiaRequest(username, gitUrl, gitUsername, gitPassword, commitA, commitB, proPathA, proPathB,
				forceReload);
	}

	public final @NotNull Path getProPathA(@NotNull Path rootPath) {
		return Utils.toPath(rootPath, proPathA);
	}

	public final @NotNull Path getProPathB(@NotNull Path rootPath) {
		return Utils.toPath(rootPath, proPathB);
	}

	public final boolean isForceReload() {
		return forceReload;
	}

	@Override
	public boolean equals(@Nullable Object object) {
		if (!super.equals(object)) return false;
		final CiaRequest that = (CiaRequest) object;
		return Arrays.equals(proPathA, that.proPathA)
				&& Arrays.equals(proPathB, that.proPathB)
				&& forceReload == that.forceReload;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + Arrays.hashCode(proPathA);
		result = 31 * result + Arrays.hashCode(proPathB);
		result = 31 * result + Boolean.hashCode(forceReload);
		return result;
	}
}
