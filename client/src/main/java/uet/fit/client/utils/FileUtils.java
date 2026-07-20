package uet.fit.client.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;

public class FileUtils {
	/**
	 * Doc noi dung file
	 *
	 * @param filePath duong dan tuyet doi file
	 * @return noi dung file
	 */
	public static String read(String filePath) {
		StringBuilder fileData = new StringBuilder(3000);
		try {
			BufferedReader reader;
			reader = new BufferedReader(new FileReader(filePath));
			char[] buf = new char[1024];
			int numRead;
			while ((numRead = reader.read(buf)) != -1) {
				String readData = String.valueOf(buf, 0, numRead);
				fileData.append(readData);
				buf = new char[1024];
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			return fileData.toString();
		}
	}

	public static void write(String content, File file) {
		write(content, file.getAbsolutePath());
	}

	public static void write(String content, String filePath) {
		try {
			new File(filePath).getParentFile().mkdirs();
			PrintWriter out = new PrintWriter(filePath);
			out.println(content);
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
