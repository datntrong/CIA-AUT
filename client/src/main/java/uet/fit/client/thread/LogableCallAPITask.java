package uet.fit.client.thread;

import uet.fit.client.logger.LogSubscriber;

import java.util.UUID;

public abstract class LogableCallAPITask<T> extends CallAPITask<T> {

	private final UUID id;

	protected String username;

	protected LogableCallAPITask(String username) {
		this.id = UUID.randomUUID();
		this.username = username;
	}

	@Override
	protected T call() throws Exception {
		try (final LogSubscriber ignored = new LogSubscriber(username, id)) {
			return super.call();
		}
	}
}
