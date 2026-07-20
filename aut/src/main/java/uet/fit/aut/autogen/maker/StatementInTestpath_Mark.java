package uet.fit.aut.autogen.maker;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class StatementInTestpath_Mark {
	private Set<Property_Marker> properties = new HashSet<>();

	public StatementInTestpath_Mark() {

	}

	public StatementInTestpath_Mark(Property_Marker property) {
		this.properties.add(property);
	}

	public StatementInTestpath_Mark(Property_Marker[] properties) {
        Collections.addAll(this.properties, properties);
	}

	public Set<Property_Marker> getProperties() {
		return properties;
	}

	public void setProperties(Set<Property_Marker> properties) {
		this.properties = properties;
	}

	public Property_Marker getPropertyByName(String name) {
		for (Property_Marker property : properties)
			if (property.getKey().equals(name))
				return property;
		return null;
	}

	@Override
	public String toString() {
		StringBuilder output = new StringBuilder();
		for (Property_Marker property : properties)
			output.append(property.toString());
		return output.toString();
	}
}
