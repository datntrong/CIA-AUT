package uet.fit.dto.test.data;

import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StubSubprogramTestNode extends SubprogramTestNode {

	@Expose
	private boolean isStub;
}
