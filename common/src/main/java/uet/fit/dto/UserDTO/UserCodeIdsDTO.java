package uet.fit.dto.UserDTO;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uet.fit.dto.DTO;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserCodeIdsDTO extends DTO {
	@Expose
	private List<String> ids = new ArrayList<>();
}
