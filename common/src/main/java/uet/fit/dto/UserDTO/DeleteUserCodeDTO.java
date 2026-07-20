package uet.fit.dto.UserDTO;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uet.fit.dto.DTO;
import uet.fit.dto.test.DeletedTestEntry;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DeleteUserCodeDTO extends DTO {
	@Expose
	private List<DeleteUserCodeEntry> list = new ArrayList<>();
}
