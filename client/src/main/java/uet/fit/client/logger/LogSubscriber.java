package uet.fit.client.logger;

import org.jetbrains.annotations.NotNull;
import uet.fit.client.thread.callback.OnReceiveLogListener;
import uet.fit.client.ui.controller.BaseController;
import uet.fit.client.utils.HttpUtils;

import java.util.UUID;

public class LogSubscriber implements AutoCloseable {
	private final @NotNull UUID taskId;

	public LogSubscriber(@NotNull String username, @NotNull UUID taskId) {
		this.taskId = taskId;
		long requestTime = System.currentTimeMillis();
		WaitingLogs.getInstance().put(taskId, requestTime);

		// avoid multiple subscriber
		if (WaitingLogs.getInstance().size() == 1)
			HttpUtils.subscribeLog(username, new OnReceiveLogListener());

		BaseController.getInstance().refreshTimer();
	}

	@Override
	public void close() {
		WaitingLogs.getInstance().remove(taskId);
		BaseController.getInstance().refreshTimer();
	}
}
