package uet.fit.dto.env;

import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.Setter;
import uet.fit.dto.DTO;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CreateEnvironmentDTO extends DTO {
	@Expose
	private String proFile;
	@Expose
	private String coverageType;
	@Expose
	private String name;
	@Expose
	private String gitUrl;
	@Expose
	private String version;
}
