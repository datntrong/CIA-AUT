package uet.fit.client.thread.callback;

import uet.fit.dto.logger.RapidlyEntry;

import java.util.List;

public interface OnReceiveLog {
	void onSucceeded(List<RapidlyEntry> entries);
	void onFailed(Throwable error);
}
