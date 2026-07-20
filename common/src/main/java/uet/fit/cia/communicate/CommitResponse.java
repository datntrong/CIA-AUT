package uet.fit.cia.communicate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

public class CommitResponse implements Serializable {
	private static final long serialVersionUID = -1L;

	private final int timeStamp;
	private final @NotNull String commitHash;
	private final @NotNull String author;
	private final @NotNull String message;
	private final @NotNull String @NotNull [] refs;

	CommitResponse(int timeStamp, @NotNull String commitHash, @NotNull String author, @NotNull String message,
			@NotNull String @NotNull [] refs) {
		this.commitHash = commitHash;
		this.author = author;
		this.timeStamp = timeStamp;
		this.message = message;
		this.refs = refs;
	}

	public static @NotNull CommitResponse of(int timeStamp, @NotNull String commitHash, @NotNull String author,
			@NotNull String message, @NotNull String @NotNull [] refs) {
		return new CommitResponse(timeStamp, commitHash, author, message, refs);
	}

	public final int getTimeStamp() {
		return timeStamp;
	}

	public final @NotNull String getCommitHash() {
		return commitHash;
	}

	public final @NotNull String getAuthor() {
		return author;
	}

	public final @NotNull String getMessage() {
		return message;
	}

	public final @NotNull String @NotNull [] getRefs() {
		return refs;
	}

	@Override
	public boolean equals(@Nullable Object object) {
		if (this == object) return true;
		if (object == null || getClass() != object.getClass()) return false;
		final CommitResponse that = (CommitResponse) object;
		return timeStamp == that.timeStamp
				&& commitHash.equals(that.commitHash)
				&& author.equals(that.author)
				&& message.equals(that.message)
				&& Arrays.equals(refs, that.refs);
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(timeStamp, commitHash, author, message);
		result = 31 * result + Arrays.hashCode(refs);
		return result;
	}
}

