package uet.fit.dto.repo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocalCheckoutDTO {

	private String gitUrl;

	private String commit;

	private String[] proFiles;
}
