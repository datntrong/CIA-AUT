package uet.fit.server.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ServerExecutors {

	private static final List<ExecutorService> executors = new ArrayList<>();

	private static ExecutorService longTermEs;

	private static ExecutorService shortTermEs;

	public static ExecutorService longTermExecutor() {
		if (longTermEs == null) {
			longTermEs = new ThreadPoolExecutor(0, 1,
					120, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
			executors.add(longTermEs);
		}
		return longTermEs;
	}

	public static ExecutorService shortTermExecutor() {
		if (shortTermEs == null) {
			shortTermEs = Executors.newWorkStealingPool();
			executors.add(shortTermEs);
		}
		return shortTermEs;
	}

	public static void shutdownAll() {
		for (ExecutorService es : executors) {
			try {
				es.shutdownNow();
			} catch (Exception ignored) {

			}
		}
	}
}
