package uet.fit.dto.UserDTO;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uet.fit.dto.DTO;
import uet.fit.dto.UserDTO.UserTypedefRow;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserCodeDTO extends DTO {

	//update laij
//	public UserCodeDTO(String environment, String file, String uut, String sut, List<UserTypedefRow> data) {
//		this
//	}
//	@Expose
//	private String userCodeId = UUID.randomUUID().toString();

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


	/**
	 * new value
	 */
	@Expose
	private List<UserTypedefRow> listContentUserCode;

}