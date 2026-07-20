package uet.fit.dto.deserializer;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import uet.fit.dto.logger.RapidlyEntry;

import java.util.ArrayList;
import java.util.List;

public final class LogImporter {

	public static List<RapidlyEntry> fromJson(String json) {
		List<RapidlyEntry> entries = new ArrayList<>();
		try {
			JsonArray jsonArray = JsonParser.parseString(json).getAsJsonArray();
			for (JsonElement jsonElement : jsonArray) {
				try {
					JsonObject jsonObject = jsonElement.getAsJsonObject();
					if (jsonObject.has("clazz")) {
						String clazz = jsonObject.get("clazz").getAsString();
						RapidlyEntry entry = (RapidlyEntry) new Gson().fromJson(jsonElement, Class.forName(clazz));
						entries.add(entry);
					}
				} catch (ClassNotFoundException ignored) {

				}
			}
		} catch (Exception ignored) {

		}
		return entries;
	}
}
