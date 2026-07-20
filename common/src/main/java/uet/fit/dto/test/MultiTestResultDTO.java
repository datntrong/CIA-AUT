package uet.fit.dto.test;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uet.fit.dto.ReportDTO;
import uet.fit.dto.test.TestRow;

import java.util.ArrayList;
import java.util.List;

@Setter
@NoArgsConstructor
public class MultiTestResultDTO extends TestResultDTO {

	@Expose
	private List<Entry> tests = new ArrayList<>();

	public MultiTestResultDTO(List<Entry> tests, Float coverage, ReportDTO report) {
		super(coverage, report);
		this.tests = tests;
	}

	public Entry[] getTests() {
		return tests.toArray(new Entry[0]);
	}
}
