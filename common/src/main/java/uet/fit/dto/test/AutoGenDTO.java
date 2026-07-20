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
public class AutoGenDTO extends DTO {

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

	@Expose
	private String strategy;

	/**
	 * Environment name
	 */
	@Expose
	private String environment;

	public boolean haveSut() {
		return sut != null && !sut.isBlank();
	}

	public static final String CFDS = "CFDS";
	public static final String USER_CODE = "USER_CODE";
	public static final String BASIS_PATH = "BASIS_PATH";
}