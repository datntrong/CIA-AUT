package uet.fit.report.html.element;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CoverageLOCElement extends CoverageElement{
	private int loc;
	private int visitedLoc;
}
