package uet.fit.aut.config.pro;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProSourceNode extends ProFileNode {
	public ProSourceNode(String path, String absolutePath) {
		super(path, absolutePath);
	}
}
