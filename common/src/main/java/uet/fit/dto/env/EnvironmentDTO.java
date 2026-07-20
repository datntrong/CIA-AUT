package uet.fit.dto.env;

import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.Setter;
import uet.fit.dto.DTO;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class EnvironmentDTO extends DTO implements INavigableNode {

	@Expose
	private String proFile;
	@Expose
	private String coverageType;
	@Expose
	private String name;
	@Expose
	private String repository;
	@Expose
	private List<Source> sources;

	// for version checking
	@Expose
	private String gitUrl;
	@Expose
	private String commit;

	private String path;
	private String project;

	public EnvironmentDTO() {
		sources = new ArrayList<>();
	}

	@Override
	public String getTitle() {
		return name;
	}

	@Override
	public List<? extends INavigableNode> getChildren() {
		return sources;
	}

	@Override
	public String toString() {
		return getTitle();
	}
}
