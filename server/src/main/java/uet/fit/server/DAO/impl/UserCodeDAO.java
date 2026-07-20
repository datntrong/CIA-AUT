package uet.fit.server.DAO.impl;

import org.jetbrains.annotations.NotNull;
import uet.fit.server.DAO.IUserCodeDAO;
import uet.fit.server.DAO.entity.UserCodeEntity;

import java.util.List;

public class UserCodeDAO extends DAO<UserCodeEntity> implements IUserCodeDAO {

	@Override
	protected @NotNull UserCodeEntity newInstance() {
		return new UserCodeEntity();
	}
	@Override
	public List<UserCodeEntity> getAllByFuncAndEnv(@NotNull String funcName, @NotNull String env) throws Exception {
		String sql = "SELECT * FROM user_code WHERE function_path = ? AND environment = ?";
		return get(sql, funcName, env);
	}

	@Override
	public @NotNull List<UserCodeEntity> searchByFunc(@NotNull String sut) throws Exception {
		String query = "SELECT * FROM user_code WHERE function_path LIKE \"%" + sut + "\" ORDER BY var_name";
		return this.get(query);
	}

	@NotNull
	public List<UserCodeEntity> searchByName(@NotNull String environment, @NotNull String sut,
			@NotNull String name) throws Exception {
		return null;
	}


}
