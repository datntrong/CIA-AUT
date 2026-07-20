package uet.fit.client.thread;

import javax.ws.rs.core.Response;

public abstract class CallAPITask<T> extends AbstractTask<T> {

	protected abstract Response request() throws Exception;
	protected abstract T toEntity(String json);

	@Override
	protected T call() throws Exception {
		Response response = request();
		int statusCode = response.getStatus();
		if (statusCode == 200) {
			String json = response.readEntity(String.class);
			return toEntity(json);
		} else {
			String error = response.readEntity(String.class);
			String message = error.isBlank() ? "Server return status code " + statusCode : error;
			throw new Exception(message);
		}
	}
}
