package uet.fit.report.html.element;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class CoverageElement {
	protected String name;
	protected int total;
	protected int pass;
	protected String status;
}
