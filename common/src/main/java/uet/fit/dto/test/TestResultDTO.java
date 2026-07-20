package uet.fit.dto.test;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import uet.fit.dto.ReportDTO;

@Getter
@Setter
public abstract class TestResultDTO {

	@Expose
	private Float coverage;

	@Expose
	private ReportDTO report;

	protected TestResultDTO() {

	}

	protected TestResultDTO(Float coverage, ReportDTO report) {
		this.coverage = coverage;
		this.report = report;
	}

	public abstract Entry[] getTests();

	@Getter
	@Setter
	@AllArgsConstructor
	public static class Entry {
		@Expose
		private String id;
		@Expose
		private String status;
		@Expose
		private Float coverage;
	}
}
