package uet.fit.dto.env;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DeletedEnvironmentEntry {

	@Expose
	private String name;

	@Expose
	private Status status;

	public enum Status {
		FAILED,
		SUCCESS,
		PERMISSION_DENY
	}
}
