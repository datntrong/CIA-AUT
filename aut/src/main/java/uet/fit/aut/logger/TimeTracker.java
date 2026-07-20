package uet.fit.aut.logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

public class TimeTracker {

	private static String getPath() {
		return Locations.getHome() + File.separator + "time.csv";
	}

	public static void add(String key, long time) {
		time = time / 1000;

		try {
			if (!new File(getPath()).exists())
				new File(getPath()).createNewFile();

			FileWriter fw = new FileWriter(getPath(), true);
			BufferedWriter bw = new BufferedWriter(fw);
			PrintWriter pw = new PrintWriter(bw);
			String line = String.format("\"%s\",\"%ds\"", key, time);
			pw.println(line);
			pw.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
