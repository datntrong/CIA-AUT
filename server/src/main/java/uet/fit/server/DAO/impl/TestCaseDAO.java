package uet.fit.server.DAO.impl;

import org.jetbrains.annotations.NotNull;
import uet.fit.server.DAO.ITestCaseDAO;
import uet.fit.server.DAO.entity.TestCaseEntity;

import java.util.List;

public class TestCaseDAO extends DAO<TestCaseEntity> implements ITestCaseDAO {

	@NotNull
	@Override
	protected TestCaseEntity newInstance() {
		return new TestCaseEntity();
	}

	@NotNull
	public List<TestCaseEntity> searchByName(@NotNull String environment, @NotNull String sut,
			@NotNull String name) throws Exception {
		String query = "SELECT * FROM test_case WHERE environment = \""
				+ environment  +"\" AND function_path LIKE \"%" + sut + "\" AND name = \"" + name + "\"";
		return this.get(query);
	}

	@Override
	public List<TestCaseEntity> getAllByFuncAndEnv(@NotNull String funcPath, @NotNull String env) throws Exception {
		String sql = "SELECT * FROM test_case WHERE function_path = ? AND environment = ?";
		return get(sql, funcPath, env);
	}

	@Override
	public List<TestCaseEntity> getAllByEnv(@NotNull String env) throws Exception {
		String sql = "SELECT * FROM test_case WHERE environment = ?";
		return get(sql, env);
	}

	@Override
	public @NotNull List<TestCaseEntity> searchByFunc(@NotNull String sut) throws Exception {
		String query = "SELECT * FROM test_case WHERE function_path LIKE \"%" + sut + "%\"";
		return this.get(query);
	}
}
