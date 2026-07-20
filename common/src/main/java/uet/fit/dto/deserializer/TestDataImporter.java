package uet.fit.dto.deserializer;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import uet.fit.dto.test.data.IHaveExpectNode;
import uet.fit.dto.test.data.ITestNode;
import uet.fit.dto.test.data.TestDataDTO;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public final class TestDataImporter {

	public static TestDataDTO fromJson(String json) {
		return new Gson()
				.newBuilder()
				.registerTypeAdapter(TestDataDTO.class, new TestDataDeserializer())
				.registerTypeAdapter(ITestNode.class, new TestDataDeserializer())
				.create()
				.fromJson(json, TestDataDTO.class);
	}

	public static class TestDataDeserializer implements JsonDeserializer<ITestNode> {
		@Override
		public ITestNode deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
			try {
				JsonObject jsonObject = jsonElement.getAsJsonObject();

				ITestNode node;
				if (jsonObject.get("clazz") == null)
					node = new TestDataDTO();
				else {
					String clazz = jsonObject.get("clazz").getAsString();
					node = (ITestNode) Class.forName(clazz).getDeclaredConstructor().newInstance();
				}

				mapFields(node, jsonObject);

				if (jsonObject.get("children") != null) {
					for (JsonElement childJson : jsonObject.get("children").getAsJsonArray()) {
						ITestNode child = context.deserialize(childJson, ITestNode.class);
						node.getChildren().add(child);
						child.setParent(node);
					}
				}

				// expected map
				if (jsonObject.get("expectNodes") != null && node instanceof IHaveExpectNode) {
					IHaveExpectNode haveExpectNode = (IHaveExpectNode) node;
					for (JsonElement childJson : jsonObject.get("expectNodes").getAsJsonArray()) {
						ITestNode expectNode = context.deserialize(childJson, ITestNode.class);
						ITestNode actualNode = node.getChildren().stream()
								.filter(n -> n.getTitle().equals(expectNode.getTitle()))
								.findFirst()
								.orElse(null);
						if (actualNode != null) {
							expectNode.setParent(node);
							haveExpectNode.getExpectNodes().add(expectNode);
							haveExpectNode.getExpectedMap().put(actualNode, expectNode);
						}
					}
				}

				return node;
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}

		private void mapFields(ITestNode node, JsonObject jsonObject) throws Exception {
			for (Field field : getAllFields(node.getClass())) {
				field.setAccessible(true);
				String fieldName = field.getName();

				if (fieldName.equals("children") || fieldName.equals("expectedMap") || fieldName.equals("expectNodes")
						|| fieldName.equals("clazz") || fieldName.equals("parent"))
					continue;

				JsonElement element = jsonObject.get(fieldName);
				if (element instanceof JsonPrimitive) {
					Class<?> fieldType = field.getType();
					if (fieldType.equals(String.class)) {
						String value = element.getAsString();
						field.set(node, value);
					} else if (fieldType.equals(Boolean.class) || fieldType.equals(boolean.class)) {
						boolean value = element.getAsBoolean();
						field.set(node, value);
					} else if (fieldType.equals(Integer.class) || fieldType.equals(int.class)) {
						int value = element.getAsInt();
						field.set(node, value);
					} else if (fieldType.equals(Float.class) || fieldType.equals(float.class)) {
						float value = element.getAsFloat();
						field.set(node, value);
					} else
						throw new Exception("Unsupported json type");
				} else if (element instanceof JsonArray) {
					JsonArray items = element.getAsJsonArray();
					String[] values = new String[items.size()];
					int i = 0;
					for (JsonElement item : items) {
						String value = item.getAsString();
						values[i] = value;
						i++;
					}
					field.set(node, values);
				}
			}
		}

		private Field[] getAllFields(Class<?> cls) {
			List<Field> fields = new ArrayList<>();

			Class<?> current = cls;
			while (current.getSuperclass() != null) { // we don't want to process Object.class
				// do something with current's fields
				fields.addAll(List.of(current.getDeclaredFields()));
				current = current.getSuperclass();
			}

			return fields.toArray(new Field[0]);
		}
	}
}
