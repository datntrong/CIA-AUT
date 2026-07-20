package uet.fit.client.thread;

public class AUTThread extends Thread {

	private final AbstractTask<?> task;

	public AUTThread(AbstractTask<?> task) {
		super(task);
		this.task = task;
	}

	@Override
	public void interrupt() {
		super.interrupt();
		if (task != null)
			task.cancel();
	}

}
