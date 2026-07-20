package uet.fit.aut.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ResourceFileUtils {

	private static final Logger logger = LoggerFactory.getLogger(Utils.class);

	public static void copyAndExtract(String dest, String zipInRes) throws IOException {
		byte[] buffer = new byte[1024];
		final File destDir = new File(dest);

		InputStream is = ResourceFileUtils.class.getResourceAsStream(zipInRes);
		assert is != null;
		ZipInputStream zis = new ZipInputStream(is);
		ZipEntry ze;
		while ((ze = zis.getNextEntry()) != null) {
			// do something with the entry - for example, extract the data
			logger.debug("Unzipping " + ze.getName());
			File newFile = newFile(destDir, ze);
			if (ze.isDirectory()) {
				if (!newFile.isDirectory() && !newFile.mkdirs()) {
					throw new IOException("Failed to create directory " + newFile);
				}
			} else {
				// fix for Windows-created archives
				File parent = newFile.getParentFile();
				if (!parent.isDirectory() && !parent.mkdirs()) {
					throw new IOException("Failed to create directory " + parent);
				}

				// write file content
				FileOutputStream fos = new FileOutputStream(newFile);
				int len;
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}
				fos.close();

				Utils.chmod777(newFile);
			}
		}
		zis.close();
	}

	private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
		File destFile = new File(destinationDir, zipEntry.getName());

		String destDirPath = destinationDir.getCanonicalPath();
		String destFilePath = destFile.getCanonicalPath();

		if (!destFilePath.startsWith(destDirPath + File.separator)) {
			throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
		}

		return destFile;
	}

	public static void copyFile(String dest, String srcInRes) throws IOException {
		FileInputStream fis = (FileInputStream) ResourceFileUtils.class.getResourceAsStream(srcInRes);
		assert fis != null;
		BufferedInputStream bis = new BufferedInputStream(fis);

		FileOutputStream fos = new FileOutputStream(dest);
		BufferedOutputStream bos = new BufferedOutputStream(fos);

		int b;
		while ((b = bis.read()) != -1) {
			bos.write(b);
		}

		bos.flush();
		bos.close();

		String[] args = new String[] {"/bin/bash", "-c", "chmod u+x " + dest, "with", "args"};
		new ProcessBuilder(args).start();
	}
}
