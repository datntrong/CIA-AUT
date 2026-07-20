package uet.fit.aut.ter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.util.SpecialCharacter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringJoiner;
import java.util.concurrent.Callable;

public class StreamReader implements Callable<String> {

	private static final Logger logger = LoggerFactory.getLogger(StreamReader.class);

	private final InputStream is;

	public StreamReader(InputStream is) {
		this.is = is;
	}

	private OnResponseListener listener;

	private String response;

	@Override
	public String call() {
		final StringJoiner joiner = new StringJoiner(SpecialCharacter.LINE_BREAK);
		try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
			while (true) {
				String s = br.readLine();

				if (s == null)
					break;

				joiner.add(s);

				if (listener != null)
					listener.receive(s);
			}
		} catch (IOException ex) {
			logger.error(ex.getMessage(), ex);
		} finally {
			if (response == null)
				response = joiner.toString();
			else
				response += SpecialCharacter.LINE_BREAK + joiner;
		}

		return response;
	}

	public void setListener(OnResponseListener listener) {
		this.listener = listener;
	}

	public String getResponse() {
		return response;
	}
}