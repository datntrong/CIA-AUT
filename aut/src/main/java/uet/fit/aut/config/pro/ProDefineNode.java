package uet.fit.aut.config.pro;

public class ProDefineNode extends ProNode {

	private final String name;
	private final String value;

	public ProDefineNode(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "[" + getClass().getSimpleName() + "]: " + name + (value == null ? "" : "=" + value);
	}
}
