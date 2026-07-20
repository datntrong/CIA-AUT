package uet.fit.aut.logger;


import org.apache.log4j.Level;
import org.apache.log4j.Logger;

@Deprecated
public class AUTLogger extends Logger {

	private static final String FQCN = AUTLogger.class.getName();

	protected AUTLogger(String name) {
		super(name);
//		try {
//			SimpleLayout layout = new SimpleLayout();
//			FileAppender appender = new FileAppender(layout, "aut." + new Date(), false);
//			addAppender(appender);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}

	public static AUTLogger get(Class<?> c) {
		Logger root = Logger.getRootLogger();

		AUTLogger logger = new AUTLogger(c.getName());

		logger.repository = root.getLoggerRepository();
		logger.parent = root;

		return logger;
	}

	@Override
	public void debug(Object message) {
		message = "[" + Thread.currentThread().getName() + "] " + message;
		if (!this.repository.isDisabled(10000)) {
			if (Level.DEBUG.isGreaterOrEqual(this.getEffectiveLevel())) {
				this.forcedLog(FQCN, Level.DEBUG, message, null);
			}
		}
	}

	@Override
	public void error(Object message) {
		error(message, null);
	}

	@Override
	public void error(Object message, Throwable t) {
		message = "[" + Thread.currentThread().getName() + "] " + message;
		if (!this.repository.isDisabled(40000)) {
			if (Level.ERROR.isGreaterOrEqual(this.getEffectiveLevel())) {
				this.forcedLog(FQCN, Level.ERROR, message, t);
			}
		}
	}

	@Override
	public void info(Object message) {
		message = "[" + Thread.currentThread().getName() + "] " + message;
		if (!this.repository.isDisabled(20000)) {
			if (Level.INFO.isGreaterOrEqual(this.getEffectiveLevel())) {
				this.forcedLog(FQCN, Level.INFO, message, null);
			}
		}
	}

}
