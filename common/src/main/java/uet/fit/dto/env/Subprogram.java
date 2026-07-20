package uet.fit.dto.env;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Subprogram implements INavigableNode {

	@Expose
	private String name;

	@Override
	public String getTitle() {
		return name;
	}

	@Override
	public List<? extends INavigableNode> getChildren() {
		return null;
	}

	@Override
	public String toString() {
		return getTitle();
	}
}
