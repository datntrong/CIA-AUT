package uet.fit.dto.logger;

import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class RapidlyEntry {

	@Expose
	private String clazz = getClass().getName();

	@Expose
	protected long time;
}
