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
public class ExportTestDTO {

	@Expose
	private String filePath;

	@Expose
	private String testCaseId;

	@Expose
	private String testCaseName;

	@Expose
	private String uut;

	@Expose
	private String sut;

}
