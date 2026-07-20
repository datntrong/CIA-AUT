package uet.fit.aut.config.pro;

public abstract class ProFileNode extends ProNode {

	protected final String path;
	protected final String absolutePath;

	public ProFileNode(String path, String absolutePath){
		this.path = path;
		this.absolutePath = absolutePath;
	}

	public String getAbsolutePath() {
		return absolutePath;
	}

	public String getPath() {
		return path;
	}

	@Override
	public String toString() {
		return "[" + getClass().getSimpleName() + "]: " + path;
	}
}
