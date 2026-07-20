package uet.fit.dto.logger;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LogDTO extends RapidlyEntry {

	public static final byte TYPE_DEB = 0; // normal log (debug)
	public static final byte TYPE_INF = 1; // success info log (blue bold text)
	public static final byte TYPE_ERR = -1; // error log (red bold text)

	@Expose
	private byte type;
	@Expose
	private Position position;
	@Expose
	private String msg;

	public LogDTO(byte type, Position position, String msg, long time) {
		this(type, position, msg);
		this.time = time;
	}

	public enum Position {
		ENVIRONMENT,
		TEST_GENERAL,
		TEST_BUILD
	}
}
