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
public class ProgressDTO extends RapidlyEntry {

	@Expose
	private String id;

	@Expose
	private String title;

	@Expose
	private int current;

	@Expose
	private int total;

	public ProgressDTO(String id, String title, int current, int total, long time) {
		this(id, title, current, total);
		this.time = time;
	}
}
