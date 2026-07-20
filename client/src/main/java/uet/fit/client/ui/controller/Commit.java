package uet.fit.client.ui.controller;

import uet.fit.util.Utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class Commit {
	private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private String hash;
	private String author;
	private String content;
	private Date date;

	public String getHash() {
		return hash;
	}

	public String getShortHash() {
		return Utils.shortenCommitHash(hash);
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Date getDate() {
		return date;
	}

	public String getFormatDate() {
		return FORMAT.format(date);
	}

	public void setDate(Date date) {
		this.date = date;
	}
}
