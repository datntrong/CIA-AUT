package uet.fit.dto.test;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DeletedTestEntry {

	@Expose
	private String id;

	@Expose
	private Status status;

	public enum Status {
		FAILED,
		SUCCESS,
		PERMISSION_DENY
	}
}
