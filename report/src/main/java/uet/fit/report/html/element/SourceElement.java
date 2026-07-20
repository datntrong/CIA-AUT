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
public class SourceElement extends CoverageLOCElement {
	private List<FunctionElement> functions = new ArrayList<>();
	protected float coverage;
}
