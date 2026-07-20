package uet.fit.client.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import uet.fit.client.utils.ConfigLocation;
import uet.fit.client.utils.FileUtils;

import java.io.File;
import java.io.IOException;

public class User {

	private static final String USERNAME_FILE_PATH = ConfigLocation.BASE_DIR + File.separator + "username.json";

	private static User instance;

	@Expose
	private String username;
	private String password;

	private final Git git = new Git();

	public static User getInstance() {
		if (instance == null) {
			File user = new File(USERNAME_FILE_PATH);
			if (!user.exists()) {
				try {
					user.getParentFile().mkdir();
					user.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
				instance = new User();
			} else {
				String json = FileUtils.read(USERNAME_FILE_PATH);
				if (json.isEmpty()) {
					instance = new User();
				} else {
					instance = new Gson().fromJson(json, User.class);
				}
			}
		}
		return instance;
	}

	public void toJson() {
		String json = new GsonBuilder()
				.excludeFieldsWithoutExposeAnnotation()
				.setPrettyPrinting()
				.create()
				.toJson(this);
		FileUtils.write(json, USERNAME_FILE_PATH);
	}

	public Git getGit() {
		return git;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public static class Git {

		private String name;
		private String password;

		public Git() {

		}

		public Git(String name, String password) {
			this.name = name;
			this.password = password;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public String getPassword() {
			return password;
		}
	}

}
