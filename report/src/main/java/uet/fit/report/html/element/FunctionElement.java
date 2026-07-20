package uet.fit.report.html.element;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FunctionElement extends CoverageLOCElement {
	private List<TestCaseElement> testCases = new ArrayList<>();
	protected float coverage;
}
