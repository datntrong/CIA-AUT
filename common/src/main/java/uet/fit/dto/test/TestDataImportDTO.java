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
public class TestDataImportDTO {

	@Expose
	private String sut;

	@Expose
	private String uut;

	@Expose
	private String id;

	@Expose
	private String name;
}
