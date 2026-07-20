package uet.fit.dto.test;

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
public class ViewTestDTO extends DTO {

	/**
	 * Test case id
	 */
	@Expose
	private String id;

	/**
	 * Environment name
	 */
	@Expose
	private String environment;
}
