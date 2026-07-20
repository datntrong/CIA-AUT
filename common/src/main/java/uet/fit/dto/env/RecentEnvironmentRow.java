package uet.fit.dto.env;

import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class RecentEnvironmentRow extends EnvironmentRow {

	@Expose
	private String lastOpened;

	public RecentEnvironmentRow(String name, String coverageType, String project, String author, String lastOpened) {
		super(name, coverageType, project, author);
		this.lastOpened = lastOpened;
	}

	@Override
	public String toString() {
		return String.format("Environment(%s, %s, %s)", getName(), getProject(), getCoverageType());
	}
}
