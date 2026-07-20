package uet.fit.server.DAO.impl;

import org.jetbrains.annotations.NotNull;
import uet.fit.server.DAO.IEnvironmentDAO;
import uet.fit.server.DAO.entity.EnvironmentEntity;
import uet.fit.server.DAO.util.JDBCConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EnvironmentDAO extends DAO<EnvironmentEntity> implements IEnvironmentDAO {
	@Override
	protected @NotNull EnvironmentEntity newInstance() {
		return new EnvironmentEntity();
	}

	@Override
	public List<EnvironmentEntity> getAll(String url, String commit) throws Exception {
		String sql = "SELECT * FROM environment WHERE git_url = ? AND commit = ?";
		return get(sql, url, commit);
	}

	@Override
	public Set<String> getCommits(String url) throws Exception {
		final String sql = "SELECT DISTINCT commit FROM environment WHERE git_url = ?";
		try (final Connection connection = JDBCConnection.getConnection();
				final PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
			preparedStatement.setString(1, url);
			try (final ResultSet resultSet = preparedStatement.executeQuery()) {
				final Set<String> tList = new HashSet<>();
				while (resultSet.next()) {
					tList.add(resultSet.getString("commit"));
				}
				return tList;
			}
		}
	}
}
