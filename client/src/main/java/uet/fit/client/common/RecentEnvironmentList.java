package uet.fit.client.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import uet.fit.client.utils.ConfigLocation;
import uet.fit.client.utils.FileUtils;
import uet.fit.dto.env.EnvironmentListDTO;
import uet.fit.dto.env.RecentEnvironmentRow;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.function.Consumer;

public class RecentEnvironmentList extends ArrayList<RecentEnvironmentRow> {

	public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private static final String RECENT_ENV_PATH = ConfigLocation.BASE_DIR + File.separator + "recentEnvs.json";

	private static RecentEnvironmentList instance;

	public static RecentEnvironmentList getInstance() {
		if (instance == null) {
			File recentEnvFile = new File(RECENT_ENV_PATH);
			if (!recentEnvFile.exists()) {
				try {
					recentEnvFile.getParentFile().mkdirs();
					recentEnvFile.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
				instance = new RecentEnvironmentList();
			} else {
				String json = FileUtils.read(RECENT_ENV_PATH);
				if (json.isEmpty())
					instance = new RecentEnvironmentList();
				else
					instance = new Gson().fromJson(json, RecentEnvironmentList.class);
			}
		}
		return instance;
	}

	public void filterDeleted(EnvironmentListDTO list) {
		removeIf(e -> list.stream().noneMatch(se -> se.getName().equals(e.getName())));
		toJson();
	}

	public void toJson() {
		String json = new GsonBuilder()
				.excludeFieldsWithoutExposeAnnotation()
				.setPrettyPrinting()
				.create()
				.toJson(instance);
		FileUtils.write(json, RECENT_ENV_PATH);
	}

	public void addEnv(RecentEnvironmentRow recentEnvironmentRow) {
		instance.stream()
				.filter(item -> item.getName().equals(recentEnvironmentRow.getName()))
				.findFirst()
				.ifPresentOrElse(new Consumer<RecentEnvironmentRow>() {
					@Override
					public void accept(RecentEnvironmentRow item) {
						instance.remove(item);
						instance.add(0, recentEnvironmentRow);
					}
				}, new Runnable() {
					@Override
					public void run() {
						int size = size();
						if (size > 20) {
							remove(size - 1);
						}
						instance.add(0, recentEnvironmentRow);
					}
				});
	}
}
