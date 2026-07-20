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
public class DuplicateTestDTO extends DTO {

	/**
	 * Test case's id
	 * Ex: 080805c3-3194-403f-812b-aaadc2b376c3
	 */
	@Expose
	private String testCaseId;

	@Expose
	private String name;

	@Expose
	private String environment;

	/**
	 * Relative file path
	 * Ex: action/actionprovider.cpp
	 */
	@Expose
	private String uut;

	/**
	 * Name of FunctionNode in project tree
	 * Ex: ActionProvider::startApp()
	 */
	@Expose
	private String sut;
}
