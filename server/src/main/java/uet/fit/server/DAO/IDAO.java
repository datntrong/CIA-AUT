package uet.fit.server.DAO;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uet.fit.server.DAO.entity.Entity;
import uet.fit.server.DAO.entity.EnvironmentEntity;

import java.util.List;

public interface IDAO<T extends Entity> {
	@NotNull
	List<T> getAll() throws Exception;

	@NotNull
	T getByKey(@NotNull Object key) throws Exception;

	void insert(@NotNull T t) throws Exception;

	void update(@NotNull T t) throws Exception;

	void delete(@NotNull Object key) throws Exception;
}
