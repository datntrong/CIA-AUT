package uet.fit.server.DAO.entity;

import lombok.Getter;
import lombok.Setter;
import uet.fit.server.DAO.entity.annotation.Column;
import uet.fit.server.DAO.entity.annotation.PrimaryKey;
import uet.fit.server.DAO.entity.annotation.Table;
import uet.fit.server.util.ServerConstants;

@Getter
@Setter
@Table("environment")
public class EnvironmentEntity extends Entity {

	public EnvironmentEntity() {
		version = ServerConstants.TOOL_VERSION;
	}

	public EnvironmentEntity(String name, String proFile, String project, String path,
			String coverageType, String gitUrl, String commit, String owner) {
		this();
		this.name = name;
		this.proFile = proFile;
		this.project = project;
		this.path = path;
		this.coverageType = coverageType;
		this.gitUrl = gitUrl;
		this.commit = commit;
		this.owner = owner;
	}

	@PrimaryKey
	@Column("name")
	private String name;

	@Column("pro_path")
	private String proFile;

	@Column("project")
	private String project;

	@Column("path")
	private String path;

	@Column("coverage_type")
	private String coverageType;

	@Column("git_url")
	private String gitUrl;

	@Column("commit")
	private String commit;

	@Column("owner")
	private String owner;

	@Column("version")
	private String version;

}
