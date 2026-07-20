package uet.fit.dto.test;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TestCaseFileDTO {

	@Expose
	private String uut;

	@Expose
	private String sut;

	@Expose
	private String name;

	@Expose
	private String oldEnvironment;

	@Expose
	private String testData;

}
