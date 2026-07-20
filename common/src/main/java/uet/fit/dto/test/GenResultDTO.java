package uet.fit.dto.test;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uet.fit.dto.ReportDTO;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GenResultDTO {

	@Expose
	private List<TestRow> tests = new ArrayList<>();

	@Expose
	private Float coverage;

	@Expose
	private ReportDTO report;
}
