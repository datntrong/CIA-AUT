package uet.fit.server.util;

import org.eclipse.jgit.lib.BatchingProgressMonitor;
import org.jetbrains.annotations.NotNull;
import uet.fit.server.logger.ServerLogger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class ServiceProgressMonitor extends BatchingProgressMonitor {
	private final @NotNull Map<String, Map.Entry<String, AtomicInteger>> tasks = new ConcurrentHashMap<>();
	private final @NotNull String username;

	public ServiceProgressMonitor(@NotNull String username) {
		this.username = username;
	}

	@Override
	protected void onUpdate(@NotNull String taskName, int workCurrent) {
		updateProgress(taskName, -1, 1, true);
	}

	@Override
	protected void onEndTask(@NotNull String taskName, int workCurrent) {
		updateProgress(taskName, 1, 1, false);
	}

	@Override
	protected void onUpdate(@NotNull String taskName, int workCurrent, int workTotal, int percentDone) {
		updateProgress(taskName, workCurrent, workTotal, true);
	}

	@Override
	protected void onEndTask(@NotNull String taskName, int workCurrent, int workTotal, int percentDone) {
		updateProgress(taskName, workTotal, workTotal, false);
	}

	private void updateProgress(@NotNull String taskName, int currentValue, int totalValue, boolean running) {
		final Map.Entry<String, AtomicInteger> entry = running ? tasks.computeIfAbsent(taskName,
				any -> Map.entry(UUID.randomUUID().toString(), new AtomicInteger(currentValue)))
				: tasks.remove(taskName);
		if (entry == null) return;
		final AtomicInteger value = entry.getValue();
		if (value.get() != currentValue) {
			ServerLogger.progress(username, entry.getKey(), taskName, currentValue, totalValue);
			value.set(currentValue);
		}
	}
}
