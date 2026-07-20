package uet.fit.dto.env;

import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.Setter;
import uet.fit.dto.DTO;

@Getter
@Setter
public class InstrumentDTO extends DTO {
	@Expose
	private String name;
	@Expose
	private String proPath;
}
