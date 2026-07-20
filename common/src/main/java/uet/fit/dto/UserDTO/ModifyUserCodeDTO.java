package uet.fit.dto.UserDTO;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uet.fit.dto.DTO;

import java.util.List;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ModifyUserCodeDTO extends DTO{
		/**
		 * new value
		 */
		@Expose
		private List<UserTypedefRow> listModifiedUserCode;

}
