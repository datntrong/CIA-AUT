package uet.fit.client.ui.obj;

import javafx.scene.control.CheckBox;

public class ChooseTestCase {
	private String name;
	private String status;
	private String coverage;
	private String author;
	private CheckBox choose;

	private String id;
	private String uut;
	private String sut;

	public ChooseTestCase(String name, String status, String coverage, String author, String id, String uut, String sut) {
		this.name = name;
		this.status = status;
		this.coverage = coverage;
		this.author = author;
		this.choose = new CheckBox();

		this.id = id;
		this.uut = uut;
		this.sut = sut;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getCoverage() {
		return coverage;
	}

	public void setCoverage(String coverage) {
		this.coverage = coverage;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public CheckBox getChoose() {
		return choose;
	}

	public void setChoose(CheckBox choose) {
		this.choose = choose;
	}

	public String getUut() {
		return uut;
	}

	public void setUut(String uut) {
		this.uut = uut;
	}

	public String getSut() {
		return sut;
	}

	public void setSut(String sut) {
		this.sut = sut;
	}

}
