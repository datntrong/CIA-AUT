package uet.fit.cia.communicate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FilterRequest extends RepositoryRequest {
	private static final long serialVersionUID = -1L;

	private final @NotNull FilterType type;
	private final @NotNull String filter;
	private final int startAt;
	private final int size;

	FilterRequest(@NotNull String username, @NotNull String gitUrl,
			@NotNull String gitUsername, @NotNull String gitPassword,
			@NotNull FilterType type, @NotNull String filter, int startAt, int size) {
		super(username, gitUrl, gitUsername, gitPassword);
		this.filter = filter;
		this.type = type;
		this.startAt = startAt;
		this.size = size;
	}

	public static @NotNull FilterRequest of(@NotNull String username, @NotNull String gitUrl,
			@NotNull String gitUsername, @NotNull String gitPassword,
			@NotNull FilterType type, @NotNull String filter, int startAt, int size) {
		return new FilterRequest(username, gitUrl, gitUsername, gitPassword, type, filter, startAt, size);
	}

	public final @NotNull FilterType getType() {
		return type;
	}

	public final @NotNull String getFilter() {
		return filter;
	}

	public final int getStartAt() {
		return startAt;
	}

	public final int getSize() {
		return size;
	}

	@Override
	public boolean equals(@Nullable Object object) {
		if (!super.equals(object)) return false;
		final FilterRequest that = (FilterRequest) object;
		return startAt == that.startAt
				&& size == that.size
				&& filter.equals(that.filter)
				&& type == that.type;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + filter.hashCode();
		result = 31 * result + type.hashCode();
		result = 31 * result + startAt;
		result = 31 * result + size;
		return result;
	}

	public enum FilterType {
		TAG,
		MESSAGE,
		COMMIT,
		BRANCH,
		ALL;

		public static final @NotNull List<FilterType> values = List.of(values());
	}
}
