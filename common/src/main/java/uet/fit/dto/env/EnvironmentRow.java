package uet.fit.dto.env;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EnvironmentRow {

	@Expose
	private String name;

	@Expose
	private String coverageType;

	@Expose
	private String project;

	@Expose
	private String author;
}
