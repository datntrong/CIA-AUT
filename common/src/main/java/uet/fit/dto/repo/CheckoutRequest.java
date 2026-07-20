package uet.fit.dto.repo;

import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import uet.fit.dto.DTO;

@Getter
@Setter
public class CheckoutRequest extends DTO {

	@Expose
	private String url;

	@Expose
	private String version;

	@Expose
	private String gitUsername;

	@NotNull
	private String gitPassword;
}
