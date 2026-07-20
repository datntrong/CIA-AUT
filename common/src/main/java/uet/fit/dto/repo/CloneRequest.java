package uet.fit.dto.repo;

import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.Setter;
import uet.fit.dto.DTO;

@Getter
@Setter
public class CloneRequest extends DTO {

	@Expose
	private String url;

	@Expose
	private String gitUser;

	@Expose
	private String gitPassword;
}
