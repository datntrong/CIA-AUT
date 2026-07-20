package uet.fit.server.DAO.impl;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.server.DAO.IDAO;
import uet.fit.server.DAO.entity.Entity;
import uet.fit.server.DAO.entity.annotation.Column;
import uet.fit.server.DAO.entity.annotation.Table;
import uet.fit.server.DAO.util.JDBCConnection;
import uet.fit.server.DAO.util.StatementGenerator;
import uet.fit.server.exception.EntityNotFoundException;
import uet.fit.server.exception.ObjectMappingException;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public abstract class DAO<T extends Entity> implements IDAO<T> {

	private static final Logger logger = LoggerFactory.getLogger(DAO.class);

	@NotNull
	public List<T> getAll() throws Exception {
		String sql = StatementGenerator.getAll(newInstance());
		return get(sql);
	}

	@NotNull
	public T getByKey(@NotNull Object key) throws Exception {
		String sql = StatementGenerator.getByKey(newInstance(), key);
		List<T> results = get(sql);
		if (results.isEmpty())
			throw new EntityNotFoundException(newInstance().getClass(), key);
		else
			return results.get(0);
	}

	public void insert(@NotNull T t) throws Exception {
		String sql = StatementGenerator.insert(t);
		execute(sql);
	}

	public void update(@NotNull T t) throws Exception {
		String sql = StatementGenerator.update(t);
		execute(sql);
	}

	public void delete(@NotNull Object key) throws Exception {
		String sql = StatementGenerator.delete(newInstance(), key);
		execute(sql);
	}

	@NotNull
	private T mapToObject(@NotNull ResultSet resultSet) throws Exception {
		T instance = newInstance();
		Class<?> clazz = instance.getClass();
		if (clazz.isAnnotationPresent(Table.class)) {
			for (Field field : clazz.getDeclaredFields()) {
				field.setAccessible(true);
				if (field.isAnnotationPresent(Column.class)) {
					String columnName = field.getAnnotation(Column.class).value();
					if (field.getType().equals(Integer.class)) {
						int value = resultSet.getInt(columnName);
						field.set(instance, value);
					} else if (field.getType().equals(Float.class)) {
						float value = resultSet.getFloat(columnName);
						field.set(instance, value);
					} else if (field.getType().equals(String.class)) {
						String value = resultSet.getString(columnName);
						field.set(instance, value);
					} else if (field.getType().equals(Timestamp.class)) {
						Timestamp value = resultSet.getTimestamp(columnName);
						field.set(instance, value);
					} else if (field.getType().equals(Long.class)) {
						Long value = resultSet.getLong(columnName);
						field.set(instance, value);
					} else if (field.getType().equals(Boolean.class)) {
						Boolean value = resultSet.getBoolean(columnName);
						field.set(instance, value);
					}
				}
			}
			return instance;
		}
		throw new ObjectMappingException(clazz);
	}

	@NotNull
	protected abstract T newInstance();

	// use for no select stm
	protected void execute(@NotNull String sql, Object... params) throws Exception {
		try (final Connection connection = JDBCConnection.getConnection()) {
			try (final PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
				setParam(preparedStatement, params);
				preparedStatement.executeUpdate();
			} catch (final Exception e) {
				logger.error("Can't execute " + sql, e);
				throw e;
			}
		}
	}

	// use for select stm
	protected List<T> get(@NotNull String sql, Object... params) throws Exception {
		try (final Connection connection = JDBCConnection.getConnection();
				final PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
			setParam(preparedStatement, params);
			try (final ResultSet resultSet = preparedStatement.executeQuery()) {
				final List<T> tList = new ArrayList<>();
				while (resultSet.next()) {
					tList.add(mapToObject(resultSet));
				}
				return tList;

			} catch (Exception e) {
				logger.error("Can't execute " + sql, e);
				throw e;
			}
		}
	}

	private void setParam(PreparedStatement preparedStatement, Object... params) throws SQLException {
		for (int i = 0; i < params.length; i++) {
			if (params[i] instanceof String) {
				preparedStatement.setString(i + 1, (String) params[i]);
			} else if (params[i] instanceof Integer) {
				preparedStatement.setInt(i + 1, (Integer) params[i]);
			} else if (params[i] instanceof Boolean) {
				preparedStatement.setBoolean(i + 1, (Boolean) params[i]);
			} else if (params[i] instanceof Timestamp) {
				preparedStatement.setTimestamp(i + 1, (Timestamp) params[i]);
			}
			//add them cac kieu can map _ giang
		}
	}
}
