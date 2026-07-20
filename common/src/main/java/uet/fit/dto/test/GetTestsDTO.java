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
public class GetTestsDTO extends DTO {

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
	 * pro file path
	 */
	@Expose
	private String proPath;
}
