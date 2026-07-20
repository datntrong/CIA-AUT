package uet.fit.server.DAO;

import org.jetbrains.annotations.NotNull;
import uet.fit.server.DAO.entity.TestCaseEntity;

import java.util.List;

public interface ITestCaseDAO extends IDAO<TestCaseEntity> {

	List<TestCaseEntity> getAllByFuncAndEnv(@NotNull String funcPath, @NotNull String env) throws Exception;

	List<TestCaseEntity> getAllByEnv(@NotNull String env) throws Exception;

	@NotNull
	List<TestCaseEntity> searchByFunc(@NotNull String sut) throws Exception;

	List<TestCaseEntity> searchByName(@NotNull String environment, @NotNull String sut,
			@NotNull String name) throws Exception;
}
