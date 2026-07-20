package uet.fit.dto.report;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uet.fit.dto.DTO;
import uet.fit.dto.env.Source;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GenerateOverviewReportDTO extends DTO {

	@Expose
	private String env;

	@Expose
	private List<Source> uutList;

}
