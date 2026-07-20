package uet.fit.dto.test.data;

import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class SutTestNode extends SubprogramTestNode implements IHaveExpectNode {

	@Expose
	private List<ITestNode> expectNodes = new ArrayList<>();

	private Map<ITestNode, ITestNode> expectedMap = new HashMap<>();
}
