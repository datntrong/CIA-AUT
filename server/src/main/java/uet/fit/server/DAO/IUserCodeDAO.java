package uet.fit.server.DAO;

import org.jetbrains.annotations.NotNull;
import uet.fit.server.DAO.entity.UserCodeEntity;

import java.util.List;

public interface IUserCodeDAO extends IDAO<UserCodeEntity> {
	List<UserCodeEntity> getAllByFuncAndEnv(@NotNull String funcName, @NotNull String env) throws Exception;

	@NotNull
	List<UserCodeEntity> searchByFunc(@NotNull String sut) throws Exception;

	List<UserCodeEntity> searchByName(@NotNull String environment, @NotNull String sut,
			@NotNull String name) throws Exception;
}
