package uet.fit.dto.test;

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
public class ExportListDTO extends DTO {

	@Expose
	private List<ExportTestDTO> exportTestList = new ArrayList<>();

}
