package uet.fit.aut.ter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ProcessHandler implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(ProcessHandler.class);

	public static void main(String[] args) throws IOException {
		Process p = Runtime.getRuntime().exec(new String[]{
				"make",
				"clean"
		});

		ProcessHandler handler = new ProcessHandler()
				.setProcess(p)
				.setOnError(new OnResponseListener() {
					@Override
					public void receive(String line) {
						logger.error(line);
					}
				})
				.setOnOutput(new OnResponseListener() {
					@Override
					public void receive(String line) {
						logger.debug(line);
					}
				})
				.setOnFinish(new OnFinishListener() {
					@Override
					public void onFinish(String out, String err) {
						logger.debug("DONE");
					}
				});

		new Thread(handler).start();

		logger.debug("Should print first");

	}

	/**
	 * Timeout setting
	 */
	private int timeout = -1; // default no timeout

	private OnTimeoutListener onTimeout;

	/**
	 * The target process
	 */
	private Process p;

	/**
	 * Callback function called when receive a line in output stream
	 */
	private OnResponseListener onOutput;

	/**
	 * Callback function called when receive a line in error stream
	 */
	private OnResponseListener onError;

	/**
	 * Callback function called when the process finishes
	 */
	private OnFinishListener onFinish;

	/**
	 * Callback function called when throwing an exception
	 */
	private OnExceptionListener onExcept;

	public ProcessHandler setOnError(OnResponseListener onError) {
		this.onError = onError;
		return this;
	}

	public ProcessHandler setOnOutput(OnResponseListener onOutput) {
		this.onOutput = onOutput;
		return this;
	}

	public ProcessHandler setTimeout(int seconds, OnTimeoutListener callback) {
		this.timeout = seconds;
		this.onTimeout = callback;
		return this;
	}

	public ProcessHandler setProcess(Process p) {
		this.p = p;
		return this;
	}

	public ProcessHandler setOnException(OnExceptionListener onExcept) {
		this.onExcept = onExcept;
		return this;
	}

	public ProcessHandler setOnFinish(OnFinishListener onFinish) {
		this.onFinish = onFinish;
		return this;
	}

	@Override
	public void run() {
		if (p != null) {
			ExecutorService executorService = Executors.newFixedThreadPool(2);

			try {
				StreamReader outputReader = new StreamReader(p.getInputStream());
				outputReader.setListener(onOutput);

				StreamReader errorReader = new StreamReader(p.getErrorStream());
				errorReader.setListener(onError);

				List<StreamReader> readers = Arrays.asList(outputReader, errorReader);

				if (timeout > 0) {
					for (StreamReader reader : readers)
						executorService.submit(reader);

					if (!p.waitFor(timeout, TimeUnit.SECONDS)) {
						p.destroy();
						onTimeout.onTimeout();
					}

					if (!executorService.awaitTermination(timeout, TimeUnit.SECONDS)) {
						executorService.shutdownNow();
					}
				} else {
					executorService.invokeAll(readers);
				}

				p.waitFor();

				if (onFinish != null)
					onFinish.onFinish(outputReader.getResponse(), errorReader.getResponse());

			} catch (Exception e) {
				if (onExcept != null)
					onExcept.onThrow(e);
				e.printStackTrace();
			} finally {
				executorService.shutdown();
			}
		}
	}
}
