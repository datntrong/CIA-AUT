package uet.fit.aut.util;

public interface QTConst {
	/**
	 * Starting flag in a include statement to determine
	 * whether the statement is a Qt header or not
	 *
	 * Ex: #include <QObject> - yes
	 *     #include <QtCore/qbytearray.h> - yes
	 *     #include <memory> - no
	 */
	String QTLIB_FLAG = "Q";

	/**
	 * Delimiter in include statement
	 * Ex: #include <QtCore/qdataarray.h>
	 */
	String QTLIB_DELIMITER = "/";

	/**
	 * In macos, qt folders containing header file are
	 * ends with .framework postfix
	 */
	String MACOS_QTLIB_POSTFIX = ".framework";
	String MACOS_QTVERSION_POSTFIX = "/Versions/5/Headers";

	/**
	 * Version of Qt framework
	 */
	String QT_VERSION = "/Qt5.14.2/5.14.2/";

	String QT_INSTALL_PREFIX = "QT_INSTALL_PREFIX";

	String SOURCE_LIST = "SOURCES";
	String HEADER_LIST = "HEADERS";
	String DEFINE_LIST = "DEFINES";

	String DESTDIR = "DESTDIR";

	String INCLUDEPATHS = "INCLUDEPATH";
	String DEPENDPATHS = "DEPENDPATH";
	String VPATHS = "VPATH";
	String RESOURCES = "RESOURCES";

	String WARNING_TAG = "Project WARNING:";
	String MESSAGE_TAG = "Project MESSAGE:";
	String IGNORE_SDK_VERSION = "CONFIG += sdk_no_version_check";
	String PRO_EXTENSION = ".pro";

//	String ORIGIN_DIR = "ORIGINDIR";
	String AUT_BUILD_DIR = "aut-build";

	String IGNORE_ERROR_FLAG = "-i";

	String MOC_EXT = ".moc";
	String OUT_EXT = ".o";

//	static String appendList(String list, String var) {
//		return String.format("%s += $$%s", list, var);
//	}
//
//	static String assignVar(String var, String value) {
//		return String.format("%s = %s", var, value);
//	}
//
//	static String prefixList(String listName) {
//		return String.format("for (item, %s) { %s += $$ORIGINDIR/$$item }", listName, listName);
//	}
//
//	static String replaceList(String listName) {
//		return String.format("for (item, %s) {\n" +
//				" %s += $$ORIGINDIR/$$item\n" +
//				" %s -= $$item\n" +
//				"}", listName, listName, listName);
//	}
}
