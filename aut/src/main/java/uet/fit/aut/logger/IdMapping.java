package uet.fit.aut.logger;

import uet.fit.aut.util.Utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.UUID;

public class IdMapping extends HashMap<String, UUID> {

	private static File getIdMapFile() {
		return new File(Locations.getHome() + File.separator + "id-map.csv");
	}

	private static IdMapping instance;

	private boolean encrypt = true;

	public static IdMapping getInstance() {
		if (instance == null) {
			instance = new IdMapping();
		}
		return instance;
	}

	public String getOrCreate(String origin) {
		if (!encrypt)
			return origin;

		if (containsKey(origin)) {
			return get(origin).toString();
		} else {
			UUID id = UUID.randomUUID();
			put(origin, id);
			try {
				append(origin, id);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return id.toString();
		}
	}

	public void initialize() throws IOException {
		if (getIdMapFile().exists())
			Utils.deleteFileOrFolder(getIdMapFile());

		if (encrypt)
			getIdMapFile().createNewFile();
	}

	private void append(String origin, UUID id) throws IOException {
		FileWriter fw = new FileWriter(getIdMapFile(), true);
		BufferedWriter bw = new BufferedWriter(fw);
		PrintWriter pw = new PrintWriter(bw);
		String line = String.format("\"%s\",\"%s\"", id, origin);
		pw.println(line);
		pw.close();
	}

	public void setEncrypt(boolean encrypt) {
		this.encrypt = encrypt;
	}

	public boolean isEncrypt() {
		return encrypt;
	}
}
