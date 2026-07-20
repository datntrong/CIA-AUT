package uet.fit.dto.func;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uet.fit.dto.DTO;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ViewSourceDTO extends DTO {

	@Expose
	private String name;

	@Expose
	private String content;

	@Expose
	private FileLocation fileLocation;
}
