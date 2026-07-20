package uet.fit.aut.thread.task;

import uet.fit.aut.thread.callback.OnFailedListener;
import uet.fit.aut.thread.callback.OnSuccessListener;

import java.util.concurrent.Callable;

/**
 * Represent a task in AUT
 * @param <V>
 */
public abstract class AbstractAUTTask<V> implements Callable<V> {

	protected OnFailedListener onFailedListener;

	protected OnSuccessListener<V> onSuccessListener;

	private V result;

	@Override
	public V call() {
		try {
			this.result = run();
			if (onSuccessListener != null)
				onSuccessListener.onSuccess(result);
			return result;
		} catch (Exception e) {
			if (onFailedListener != null)
				onFailedListener.onFail(e);
			return null;
		} finally {
			System.gc();
		}
	}

	public abstract V run() throws Exception;

	public V getResult() {
		return result;
	}

	public void setOnFailed(OnFailedListener listener) {
		this.onFailedListener = listener;
	}

	public void setOnSuccess(OnSuccessListener<V> listener) {
		this.onSuccessListener = listener;
	}
}