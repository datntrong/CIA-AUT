package uet.fit.server.DAO.util;

import uet.fit.server.config.SQLConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class JDBCConnection {

	public static final String DB_NAME = "uet_fit_aut";

	public static Connection getConnection() throws SQLException, ClassNotFoundException {
		Class.forName("com.mysql.cj.jdbc.Driver");
		SQLConfig sqlConfig = SQLConfig.load();
		String url = sqlConfig.getUrl() + "/" + DB_NAME;
		String user = sqlConfig.getUser();
		String password = sqlConfig.getPassword();
		return DriverManager.getConnection(url, user, password);
	}

	public static boolean connectable() {
		try (final Connection ignored = getConnection()) {
			return true;
		} catch (SQLException | ClassNotFoundException e) {
			return false;
		}
	}
}
