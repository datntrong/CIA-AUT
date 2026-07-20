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
public class CreateTestDTO extends DTO {

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

	/**
	 * Test case name
	 */
	@Expose
	private String name;

	/**
	 * Environment name
	 */
	@Expose
	private String environment;
}
