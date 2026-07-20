package uet.fit.dto.test.data;

import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EditableTestNode extends HaveValueTestNode {

	@Expose
	private String[] choices;

	public boolean isMultipleChoices() {
		return choices != null;
	}
}
