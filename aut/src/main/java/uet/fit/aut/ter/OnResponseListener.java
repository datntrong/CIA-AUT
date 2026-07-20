package uet.fit.aut.ter;

import java.io.IOException;

public interface OnResponseListener {
	void receive(String line) throws IOException;
}
