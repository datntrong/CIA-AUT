package uet.fit.dto.test;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uet.fit.dto.DTO;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ImportTestDTO extends DTO {

	@Expose
	private String env;

	@Expose
	private String filePath;
}
