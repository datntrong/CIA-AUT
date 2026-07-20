package uet.fit.aut.thread;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

public class AutExecutors {

	private static final List<ExecutorService> executors = new ArrayList<>();

	public static ExecutorService newWorkStealingPool() {
		ExecutorService es = new ForkJoinPool();
		executors.add(es);
		return es;
	}

	public static ExecutorService newSingleExecutor() {
		ExecutorService es = Executors.newSingleThreadExecutor();
		executors.add(es);
		return es;
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
