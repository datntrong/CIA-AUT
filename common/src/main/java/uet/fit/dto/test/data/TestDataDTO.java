package uet.fit.dto.test.data;

import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uet.fit.dto.DTO;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class TestDataDTO extends DTO implements ITestNode {

	@Expose
	private String testCaseName;

	@Expose
	private String id;

//	@Expose
//	private String uut;
//
//	@Expose
//	private String sut;

	@Expose
	private List<ITestNode> children = new ArrayList<>();

	@Expose
	private boolean editable;

	@Override
	public String getTitle() {
		return "<<ROOT>>";
	}

	@Override
	public void setTitle(String title) {
		this.testCaseName = title;
	}

	@Override
	public ITestNode getParent() {
		return null;
	}

	@Override
	public void setParent(ITestNode parent) {

	}

	@Override
	public String getPath() {
		return getTitle();
	}

}
