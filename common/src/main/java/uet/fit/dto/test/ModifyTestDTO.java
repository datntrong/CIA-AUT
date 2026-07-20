package uet.fit.dto.test;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uet.fit.dto.DTO;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ModifyTestDTO extends DTO {

	/**
	 * Test case's id
	 * Ex: 080805c3-3194-403f-812b-aaadc2b376c3
	 */
	@Expose
	private String testCaseId;

	/**
	 * node path in testcase
	 * Ex: "<<ROOT>>/operation.cpp/multiple/a"
	 */
	@Expose
	private String path;

	/**
	 * new value
	 */
	@Expose
	private String value;

	@Expose
	private ChangeType changeType;

	public enum ChangeType {
		ENTER,
		ENTER_CODE,
		ASSERT,
		ASSERT_CODE,
		OTHER
	}
}
