package uet.fit.dto.UserDTO;

public class UserTypedefRow {
	private String id;
	private String myType;
	private String name ;
	private String value;

	public UserTypedefRow(String id, String myType, String name, String value) {
		this.id = id;
		this.myType = myType;
		this.name = name;
		this.value = value;
	}

	public String getMyType() {
		return myType;
	}

	public void setMyType(String myType) {
		this.myType = myType;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
