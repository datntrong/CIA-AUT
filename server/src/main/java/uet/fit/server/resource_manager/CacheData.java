package uet.fit.server.resource_manager;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uet.fit.aut.config.ProjectConfig;
import uet.fit.aut.parser.obj.ProjectNode;
import uet.fit.dto.env.Source;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CacheData {
	private List<Source> sources;
	private ProjectConfig projectConfig;
	private ProjectNode projectNode;
}
