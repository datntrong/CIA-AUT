package uet.fit.dto.test.data;

import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class HaveTypeTestNode extends TestNode {

	@Expose
	private String category;

	@Expose
	private String type;

	@Expose
	private String userCode;

	@Expose
	private String assertMethod;

	@Expose
	private String[] supportAsserts;

	public boolean isUseUserCode() {
		return userCode != null;
	}
}
