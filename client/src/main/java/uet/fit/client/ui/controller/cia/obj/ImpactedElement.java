package uet.fit.client.ui.controller.cia.obj;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uet.fit.cia.cpp.display.ProjectResponse;

import java.nio.file.Path;

public final class ImpactedElement extends ModifiedElement {
	private final double impact;

	public ImpactedElement(@Nullable Path oldPath, @Nullable Path newPath, @NotNull String function,
			@NotNull ProjectResponse.Change change, @NotNull ProjectResponse.Location location, double impact) {
		super(oldPath, newPath, function, change, location);
		this.impact = impact;
	}

	public double getImpact() {
		return impact;
	}
}
