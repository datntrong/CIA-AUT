package uet.fit.cia.communicate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;

public class ContentResponse implements Serializable {
	private static final long serialVersionUID = -1L;

	private final @NotNull String content;

	ContentResponse(@NotNull String content) {
		this.content = content;
	}

	public static @NotNull ContentResponse of(@NotNull String content) {
		return new ContentResponse(content);
	}

	public final @NotNull String getContent() {
		return content;
	}

	@Override
	public boolean equals(@Nullable Object object) {
		if (this == object) return true;
		if (object == null || getClass() != object.getClass()) return false;
		final ContentResponse that = (ContentResponse) object;
		return content.equals(that.content);
	}

	@Override
	public int hashCode() {
		return Objects.hash(content);
	}
}
