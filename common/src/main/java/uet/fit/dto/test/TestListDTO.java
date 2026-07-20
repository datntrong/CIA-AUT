package uet.fit.dto.test;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uet.fit.dto.DTO;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TestListDTO extends DTO {

	@Expose
	private String uut;

	@Expose
	private String sut;

	@Expose
	private List<TestRow> list = new ArrayList<>();
}
