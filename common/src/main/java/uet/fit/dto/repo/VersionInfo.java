package uet.fit.dto.repo;

import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VersionInfo {

	public VersionInfo(String url, String commitHash) {
		this.url = url;
		this.commitHash = commitHash;
	}

	@Expose
	private String url;

	@Expose
	private String commitHash;

	@Expose
	private int datetime;

	@Expose
	private String message;

	@Expose
	private String committer;

	@Override
	public String toString() {
		return "VersionInfo{" +
				"url='" + url + '\'' +
				", commitHash='" + commitHash + '\'' +
				", datetime=" + datetime +
				", message='" + message + '\'' +
				", committer='" + committer + '\'' +
				'}';
	}
}
