package uet.fit.server.DAO.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uet.fit.server.DAO.entity.annotation.Column;
import uet.fit.server.DAO.entity.annotation.PrimaryKey;
import uet.fit.server.DAO.entity.annotation.Table;

import java.sql.Timestamp;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table("user")
public class UserEntity extends Entity {

	@PrimaryKey
	@Column("username")
	private String name;

	@Column("password")
	private String password;

	@Column("expired_time")
	private Timestamp expiredTime;

	@Column("online")
	private Boolean online;

	public boolean isOnline() {
		return online;
	}
}
