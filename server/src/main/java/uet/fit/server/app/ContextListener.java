package uet.fit.server.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.thread.AutExecutors;
import uet.fit.server.DAO.util.JDBCConnection;
import uet.fit.server.config.SQLConfig;
import uet.fit.server.rest.ServerExecutors;
import uet.fit.server.rest.service.UserService;
import uet.fit.server.util.ServerConstants;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class ContextListener implements ServletContextListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(ContextListener.class);

	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		LOGGER.debug("Starting server ciaut uet-v" + ServerConstants.TOOL_VERSION + " up!");

		if (JDBCConnection.connectable()) {
			logoffAllAndUpdateDB();
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent servletContextEvent) {
		LOGGER.debug("Shutting down!");

		// Shutdown all executor services
		shutdownExecutors();
	}

	private void logoffAllAndUpdateDB() {
		try {
			SQLConfig.initDatabase();
			new UserService().logoffAllUsers();
		} catch (Exception ex) {
			LOGGER.error("Something wrong with the database", ex);
		}
	}

	private void shutdownExecutors() {
		LOGGER.debug("Shutdown all executor services");
		AutExecutors.shutdownAll();
		ServerExecutors.shutdownAll();
	}
}