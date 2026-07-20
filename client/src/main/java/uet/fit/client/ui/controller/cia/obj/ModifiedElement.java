package uet.fit.client.ui.controller.cia.obj;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uet.fit.cia.cpp.display.ProjectResponse;

import java.nio.file.Path;
import java.util.Objects;

public class ModifiedElement {
	private final @Nullable Path oldPath;
	private final @Nullable Path newPath;
	private final @NotNull String name;
	private final @NotNull ProjectResponse.Change change;
	private final @NotNull ProjectResponse.Location location;

	public ModifiedElement(@Nullable Path oldPath, @Nullable Path newPath, @NotNull String function,
			@NotNull ProjectResponse.Change change, @NotNull ProjectResponse.Location location) {
		this.oldPath = oldPath;
		this.newPath = newPath;
		this.name = function;
		this.change = change;
		this.location = location;
	}

	public @Nullable Path getOldPath() {
		return oldPath;
	}

	public @Nullable Path getNewPath() {
		return newPath;
	}

	public @NotNull String getName() {
		return name;
	}

	public @NotNull ProjectResponse.Change getChange() {
		return change;
	}

	public @NotNull ProjectResponse.Location getLocation() {
		return location;
	}

	@Override
	public boolean equals(@Nullable Object object) {
		if (this == object) return true;
		if (!(object instanceof ModifiedElement)) return false;
		final ModifiedElement that = (ModifiedElement) object;
		return change == that.change
				&& Objects.equals(oldPath, that.oldPath)
				&& Objects.equals(newPath, that.newPath)
				&& name.equals(that.name)
				&& location.equals(that.location);
	}

	@Override
	public int hashCode() {
		return Objects.hash(oldPath, newPath, name, change, location);
	}
}