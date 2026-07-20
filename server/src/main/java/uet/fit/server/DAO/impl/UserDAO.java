package uet.fit.server.DAO.impl;

import org.jetbrains.annotations.NotNull;
import uet.fit.aut.logger.AUTLogger;
import uet.fit.server.DAO.IUserDAO;
import uet.fit.server.DAO.entity.UserEntity;

public class UserDAO extends DAO<UserEntity> implements IUserDAO {

	@Override
	protected @NotNull UserEntity newInstance() {
		return new UserEntity();
	}

	public void logoffAll() throws Exception {
		String sql = "UPDATE user SET online = 0";
		execute(sql);
	}
}
