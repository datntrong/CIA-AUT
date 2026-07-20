package uet.fit.aut.config;

import uet.fit.aut.util.PathUtils;

import java.io.File;

import static uet.fit.aut.thread.task.ExecuteTestTask.INSTRUMENT_NAME;

public class ProFileUtils {

	/**
	 * get cloned project path to run qmake and retrieve project config
	 *
	 * @param env - environment folder
	 * @param user - username
	 * @param project - project directory
	 * @param proFile - pro file
	 */
	public static String getCloneProFile(File env, String user, File project, File proFile) {
		File clonedFolder = env.toPath().resolve(INSTRUMENT_NAME).toFile();
		File proFileInCloneFolder = new File(getProFileInClonedFolder(clonedFolder, project, proFile));
		String basePath = proFileInCloneFolder.getParent();
		String name = user + "-" + proFileInCloneFolder.getName();
		return basePath + File.separator + name;
	}

	public static File getEnvProContainer(File env, File project, File proFile) {
		File instrumentFolder = env.toPath().resolve(INSTRUMENT_NAME).toFile();
		String envProPath = getProFileInClonedFolder(instrumentFolder, project, proFile);
		File envProFile = new File(envProPath);
		return envProFile.getParentFile();
	}

	/**
	 * Get corresponding pro file in instrument folder
	 */
	public static String getProFileInClonedFolder(File instrumentDir, File origin, File proFile) {
		String relativePath = PathUtils.relative(proFile, origin);
		return PathUtils.absolute(relativePath, instrumentDir);
	}
}
