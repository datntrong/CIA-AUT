package uet.fit.dto.func;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FileLocation {
	@Expose
	private int offset;

	@Expose
	private int length;

	@Expose
	private int startLine;

	@Expose
	private int endLine;
}
