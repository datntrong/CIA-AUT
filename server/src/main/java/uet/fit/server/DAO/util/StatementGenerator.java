package uet.fit.server.DAO.util;

import uet.fit.server.DAO.entity.Entity;
import uet.fit.server.DAO.entity.annotation.Column;
import uet.fit.server.DAO.entity.annotation.PrimaryKey;
import uet.fit.server.DAO.entity.annotation.Table;
import uet.fit.server.exception.NullKeyException;
import uet.fit.server.exception.ObjectMappingException;
import uet.fit.server.exception.UnsupportedColumnTypeException;

import java.lang.reflect.Field;
import java.sql.Timestamp;

public class StatementGenerator {

	public static String getAll(Entity entity) throws ObjectMappingException {
		Class<?> clazz = entity.getClass();
		if (clazz.isAnnotationPresent(Table.class)) {
			String tableName = clazz.getAnnotation(Table.class).value();
			return "SELECT * FROM " + tableName + ";";
		}
		throw new ObjectMappingException(clazz);
	}

	public static String getByKey(Entity entity, Object key) throws Exception {
		Class<?> clazz = entity.getClass();
		if (clazz.isAnnotationPresent(Table.class)) {
			for (Field field : clazz.getDeclaredFields()) {
				field.setAccessible(true);
				if (field.isAnnotationPresent(PrimaryKey.class)) {
					String tableName = clazz.getAnnotation(Table.class).value();
					String columnName = field.getAnnotation(Column.class).value();
					String value = getValue(key);
					return "SELECT * FROM " + tableName + " WHERE " + columnName + " = " + value + ";";
				}
			}
		}
		throw new ObjectMappingException(clazz);
	}

	public static String insert(Entity entity) throws Exception {
		Class<?> clazz = entity.getClass();
		if (clazz.isAnnotationPresent(Table.class)) {
			String tableName = clazz.getAnnotation(Table.class).value();
			String sql = "INSERT INTO " + tableName + " (";
			for (Field field : clazz.getDeclaredFields()) {
				field.setAccessible(true);
				String columnName = field.getAnnotation(Column.class).value();
				sql = sql + columnName + ", ";
			}
			sql = sql.substring(0, sql.length() - 2) + ") VALUES(";
			for (Field field : clazz.getDeclaredFields()) {
				field.setAccessible(true);
				Object valueObj = field.get(entity);
				if (valueObj == null) {
					sql = sql + "NULL,";
				} else {
					String value = getValue(valueObj);
					sql = sql + value + ",";
				}
			}
			sql = sql.substring(0, sql.length() - 1) + ");";
			return sql;
		}
		throw new ObjectMappingException(clazz);
	}

	public static String update(Entity entity) throws Exception {
		Class<?> clazz = entity.getClass();
		if (clazz.isAnnotationPresent(Table.class)) {
			String tableName = clazz.getAnnotation(Table.class).value();
			String sql = "UPDATE " + tableName + " SET ";
			String condition = "";
			for (Field field : clazz.getDeclaredFields()) {
				field.setAccessible(true);
				String columnName = field.getAnnotation(Column.class).value();
				Object valueObj = field.get(entity);
				if (!field.isAnnotationPresent(PrimaryKey.class)) {
					if (valueObj != null) {
						String value = getValue(valueObj);
						sql = sql + columnName + " = " + value + ", ";
					}
				} else {
					if (valueObj == null)
						throw new NullKeyException(entity);

					String value = getValue(valueObj);
					condition = " WHERE " + columnName + " = " + value + ";";
				}
			}
			sql = sql.substring(0, sql.length() - 2) + condition;
			return sql;
		}
		throw new ObjectMappingException(clazz);
	}

	public static String delete(Entity entity, Object key) throws Exception {
		Class<?> clazz = entity.getClass();
		if (clazz.isAnnotationPresent(Table.class)) {
			String tableName = clazz.getAnnotation(Table.class).value();
			String sql = "DELETE FROM " + tableName + " WHERE ";
			for (Field field : clazz.getDeclaredFields()) {
				field.setAccessible(true);
				if (field.isAnnotationPresent(PrimaryKey.class)) {
					String columnName = field.getAnnotation(Column.class).value();
					String value = getValue(key);
					sql = sql + columnName + " = " + value + ";";
				}
				break;
			}
			return sql;
		}
		throw new ObjectMappingException(clazz);
	}

	private static String getValue(Object object) throws UnsupportedColumnTypeException {
		if (object instanceof Number) {
			return String.valueOf(object);
		} else if (object instanceof String || object instanceof Timestamp) {
			return "'" + object + "'";
		} else if (object instanceof Boolean) {
			return String.valueOf(object).toUpperCase();
		} else throw new UnsupportedColumnTypeException(object.getClass());
	}
}
