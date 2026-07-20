package uet.fit.server.config;

import org.apache.ibatis.jdbc.ScriptRunner;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.config.Config;
import uet.fit.server.DAO.entity.Entity;
import uet.fit.server.DAO.entity.EnvironmentEntity;
import uet.fit.server.DAO.entity.annotation.Column;
import uet.fit.server.DAO.entity.annotation.Table;
import uet.fit.server.DAO.util.JDBCConnection;
import uet.fit.server.rest.resource.Utils;
import uet.fit.server.util.ServerConstants;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SQLConfig {

	private static final Logger logger = LoggerFactory.getLogger(SQLConfig.class);

	private static final String SQL_PREFIX_URL = "jdbc:mysql://";

	private String host = "localhost";
	private String user = "root";
	private String password = "password";
	private String port = "3306";

	private SQLConfig() {
	}

	public static SQLConfig load() {
		SQLConfig sqlConfig = new SQLConfig();
		sqlConfig.port = String.valueOf(Config.getSqlPort());
		sqlConfig.user = Config.getSqlUser();
		sqlConfig.password = Config.getSqlPassword();
		sqlConfig.host = Config.getSqlHost();
		return sqlConfig;
	}

	private static void init() throws ClassNotFoundException, SQLException, IOException {
		logger.debug("Initialize MySQL database");

		SQLConfig sqlConfig = SQLConfig.load();

		// Registering the Driver
		Class.forName("com.mysql.cj.jdbc.Driver");
		try (final Connection connection = DriverManager.getConnection(
				sqlConfig.getUrl(), sqlConfig.getUser(), sqlConfig.getPassword());
				final StringReader reader = new StringReader(Utils.readResource("db.sql"));
				final PrintWriter errorWriter = new PrintWriter(new SQLErrorStream())) {
			final ScriptRunner runner = new ScriptRunner(connection);
			runner.setLogWriter(null);
			runner.setErrorLogWriter(errorWriter);
			runner.runScript(reader);
		}
	}

	public static void initDatabase() throws IOException, SQLException, ClassNotFoundException {
		init();
		try (final Connection connection = JDBCConnection.getConnection()) {
			clearIfOutdated(connection, EnvironmentEntity.class);

			addColumn(connection, "user", "password", "varchar(256) NOT NULL DEFAULT 'password'");
			addColumn(connection, "environment", "git_url", "text");
			addColumn(connection, "environment", "commit", "varchar(40)");
			addColumn(connection, "environment", "project", "text NOT NULL");
			addColumn(connection, "environment", "path", "text NOT NULL");
			addColumn(connection, "environment", "version", "varchar(8) NOT NULL");

			clearIfOutdated(connection, "environment", "version", ServerConstants.TOOL_VERSION);
		}
	}

	private static void clearIfOutdated(Connection connection, String table, String column, String value) throws SQLException {
		try (Statement clearStm = connection.createStatement()) {
			String clearSql = String.format("DELETE FROM %s WHERE %s <> '%s' or %s is null ;", table, column, value, column);
			clearStm.execute(clearSql);
		}
		logger.info("Clear outdated data in " + table);
	}

	private static <T extends Entity> void clearIfOutdated(Connection connection, Class<T> cls) throws SQLException {
		if (cls.isAnnotationPresent(Table.class)) {
			String tableName = cls.getAnnotation(Table.class).value();

			final List<String> newColumns = new ArrayList<>();
			for (Field field : cls.getDeclaredFields()) {
				field.setAccessible(true);
				if (field.isAnnotationPresent(Column.class)) {
					String column = field.getAnnotation(Column.class).value();
					newColumns.add(column);
				}
			}

			final List<String> oldColumns = new ArrayList<>();
			DatabaseMetaData metadata = connection.getMetaData();
			try (ResultSet resultSet = metadata.getColumns(null, JDBCConnection.DB_NAME, tableName, null)) {
				while (resultSet.next()) {
					String name = resultSet.getString("COLUMN_NAME");
					oldColumns.add(name);
				}
			}

			boolean exact = new HashSet<>(newColumns).equals(new HashSet<>(oldColumns));
			if (!exact) {
				try (Statement clearStm = connection.createStatement()) {
					String clearSql = String.format("DELETE FROM %s ;", tableName);
					clearStm.execute(clearSql);
				}
				logger.info("Clear outdated data in " + tableName);
			}
		}
	}

	private static void addColumn(@NotNull Connection connection, @NotNull String table,
			@NotNull String column, @NotNull String type) throws SQLException {
		try (Statement updateStatement = connection.createStatement()) {
			String updateSql = String.format("ALTER TABLE `%s` ADD `%s` %s ;", table, column, type);
			try {
				updateStatement.execute(updateSql);
				logger.debug("Execute " + updateSql);
			} catch (SQLSyntaxErrorException e) {
				Pattern pattern = Pattern.compile("\\QDuplicate column name\\E(.+)");
				Matcher matcher = pattern.matcher(e.getMessage());
				if (matcher.find()) {
					logger.info("Column" + matcher.group(1) + " has been already added");
				} else throw e;
			}
		}
	}

	public String getUser() {
		return user;
	}

	public String getPassword() {
		return password;
	}

	public String getUrl() {
		return SQL_PREFIX_URL + host + ":" + port;
	}

	static class SQLErrorStream extends OutputStream {
		/**
		 * The logger where to log the written bytes.
		 */
		private final Logger logger;

		/**
		 * The internal memory for the written bytes.
		 */
		private String mem;

		/**
		 * Creates a new log output stream which logs bytes to the specified logger with the specified
		 * level.
		 */
		public SQLErrorStream() {
			this.logger = SQLConfig.logger;
			this.mem = "";
		}

		/**
		 * Writes a byte to the output stream. This method flushes automatically at the end of a line.
		 *
		 * @param b DOCUMENT ME!
		 */
		public void write(int b) {
			byte[] bytes = new byte[1];
			bytes[0] = (byte) (b & 0xff);
			mem = mem + new String(bytes);

			if (mem.endsWith("\n")) {
				mem = mem.substring(0, mem.length() - 1);
				flush();
			}
		}

		/**
		 * Flushes the output stream.
		 */
		public void flush() {
			logger.error(mem);
			mem = "";
		}
	}
}