package uet.fit.dto.test;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uet.fit.dto.ReportDTO;

@Getter
@Setter
@NoArgsConstructor
public class SingleTestResultDTO extends TestResultDTO {

	@Expose
	private Entry test;

	public SingleTestResultDTO(Entry test, Float coverage, ReportDTO report) {
		super(coverage, report);
		this.test = test;
	}

	public Entry[] getTests() {
		return new Entry[] {test};
	}
}
