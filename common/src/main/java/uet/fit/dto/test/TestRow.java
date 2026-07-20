package uet.fit.dto.test;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestRow {

	@Expose
	private String id;

	@Expose
	private String name;

	@Expose
	private String status;

	@Expose
	private float coverage;

	@Expose
	private String owner;

	@Expose
	private String createdTime;
}
