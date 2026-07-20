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
@Table("test_case")
public class TestCaseEntity extends Entity {
	@PrimaryKey
	@Column("id")
	private String id;

	@Column("name")
	private String name;

	@Column("status")
	private String status;

	@Column("coverage")
	private Float coverage;

	@Column("test_data_path")
	private String testData;

	@Column("environment")
	private String env;

	@Column("owner")
	private String owner;

	@Column("function_path")
	private String function;

	@Column("created_time")
	private String createdTime;

	@Column("modified_time")
	private String lastModified;

	@Column("result_pass")
	private Integer pass;

	@Column("result_total")
	private Integer total;
}
