package uet.fit.server.DAO.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uet.fit.server.DAO.entity.annotation.Column;
import uet.fit.server.DAO.entity.annotation.PrimaryKey;
import uet.fit.server.DAO.entity.annotation.Table;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table("user_code")
public class UserCodeEntity extends Entity{
	@PrimaryKey
	@Column("id")
	private String id;

	@Column("type")
	private String type;

	@Column("var_name")
	private String varName;

	@Column("code")
	private String code;

	@Column("function_path")
	private String testData;

	@Column("environment")
	private String env;

	@Column("owner")
	private String owner;

}
