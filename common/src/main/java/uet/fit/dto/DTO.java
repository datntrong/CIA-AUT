package uet.fit.dto;
import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

@Getter
@Setter
public abstract class DTO {
	@Expose
	private String user;
}


