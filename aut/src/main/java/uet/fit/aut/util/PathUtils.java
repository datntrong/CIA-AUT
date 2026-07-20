package uet.fit.aut.util;

import uet.fit.aut.config.FolderConfig;
import uet.fit.aut.logger.AUTLogger;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathUtils {

	private static final AUTLogger logger = AUTLogger.get(PathUtils.class);

	public static boolean equals(String absolute1, String absolute2) {
		return absolute1.equals(absolute2);
	}

	public static boolean isAbsolute(String path) {
		Path p = Paths.get(path);
		return p.isAbsolute();
	}

	public static boolean isRelative(String path) {
		Path p = Paths.get(path);
		return !p.isAbsolute();
	}

	public static String normalize(String path) {
		path = path.replace("\r", "").replace("\n", "");
		String singleBackSlash = "\\";
		String doubleBackSlash = singleBackSlash + singleBackSlash;
		String singleSlash = "/";

		path = path.replace(singleBackSlash, File.separator)
				.replace(singleSlash, File.separator)
				.replace(doubleBackSlash, File.separator);

		return Paths.get(path).normalize().toString();
	}

	public static String relative(File target, File anchor) {
		Path absolutePath = Paths.get(target.toURI());

		Path anchorPath;
		if (anchor.isDirectory()) {
			anchorPath = Paths.get(anchor.toURI());
		} else {
			anchorPath = Paths.get(anchor.getParentFile().toURI());
		}

		return anchorPath.relativize(absolutePath).toString();
	}

	public static String absolute(String relative, File anchor) {
		File directory;
		if (anchor.isFile()) {
			directory = anchor.getParentFile();
		} else {
			directory = anchor;
		}
		Path absolute = Paths.get(directory.getAbsolutePath(), relative);
		return absolute.normalize().toString();
	}

	public static String doubleNormalize(String path) {
		String singleBackSlash = "\\";
		String doubleBackSlash = singleBackSlash + singleBackSlash;
		String singleSlash = "/";

		String result = normalize(path);

		if (!File.separator.equals(singleSlash)) {
			result = result.replace(File.separator, doubleBackSlash);
		}

		return result;
	}

	public static String toRelative(String absolutePath) {
		if (absolutePath == null)
			return "";
		//TODO
		String workspace = FolderConfig.load().getWorkspace();

		int index = -1;
		if (Utils.isWindows()) {
			index = workspace.indexOf("\\aka-working-space");
		} else if (Utils.isUnix() || Utils.isMac()) {
			index = workspace.indexOf("/aka-working-space");
		}

		if (index < 0) {
			logger.debug("absolutePath: " + absolutePath);
			if (Utils.isWindows()) {
				logger.debug("wanna find " + "\\aka-working-space");
			} else if (Utils.isUnix() || Utils.isMac()) {
				logger.debug("wanna find " + "/aka-working-space");
			}
			return absolutePath;
		}

		String prefix = workspace.substring(0, index);
		return absolutePath.replaceFirst("\\Q" + prefix + "\\E", ".");
	}

	public static String toAbsolute(String relativePath) {
		String absolutePath = relativePath;

		int offset = 0;

		if (relativePath.startsWith("..")) {
			offset = 2;
		} else if (relativePath.startsWith(".")) {
			offset = 1;
		}

		if (offset > 0) {
//            ProjectNode root = Environment.getInstance().getProjectNode();
//            if (root != null) {
//                absolutePath = root.getFile().getParent() + relativePath.substring(offset);
//            } else {
			String workspace = FolderConfig.load().getWorkspace();
			int index = -1;
			if (Utils.isWindows()) {
				index = workspace.indexOf("\\aka-working-space");
			} else if (Utils.isUnix() || Utils.isMac()) {
				index = workspace.indexOf("/aka-working-space");
			}
			if (index < 0) {
				logger.debug("relativePath: " + relativePath);
				if (Utils.isWindows()) {
					logger.debug("wanna find " + "\\aka-working-space");
				} else if (Utils.isUnix() || Utils.isMac()) {
					logger.debug("wanna find " + "/aka-working-space");
				}
				absolutePath = relativePath;
			}
			else {
				String prefix = workspace.substring(0, index);
				absolutePath = prefix + relativePath.substring(offset);
//                }
			}
		}

		absolutePath = Utils.doubleNormalizePath(absolutePath);

		return absolutePath;
	}

	public static String getFilename(String path) {
		if (path.contains(File.separator))
			return path.substring(path.lastIndexOf(File.separator));
		else return path;
	}
}
