package uet.fit.dto.env;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Source implements INavigableNode {

	@Expose
	protected String path;

	@Expose
	private List<Subprogram> functions = new ArrayList<>();

	public Source(String path){
		this.path = path;
	}

	@Override
	public String getTitle() {
		return path;
	}

	@Override
	public List<? extends INavigableNode> getChildren() {
		return functions;
	}

	@Override
	public String toString() {
		return getTitle();
	}
}
