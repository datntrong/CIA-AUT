package uet.fit.aut.parser.obj;

import uet.fit.aut.parser.dependency.Dependency;
import uet.fit.aut.util.PathUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class Node implements INode {

	private String name = "";

	protected String absolutePath = "";

	private List<INode> children = new ArrayList<>();

	protected INode parent = null;

	private List<Dependency> dependencies = new ArrayList<>();

	public Node() {
	}

	@Override
	public String getNewType() {
		return name;
	}

	@Override
	public INode getParent() {
		return parent;
	}

	@Override
	public void setParent(INode parent) {
		this.parent = parent;
	}

	@Override

	public String toString() {
		return getNewType();
	}

	@Override
	public INode clone() {
		try {
			return (INode) super.clone();
		} catch (CloneNotSupportedException e) {
			// e.printStackTrace();
			return null;
		}
	}

	@Override
	public String getAbsolutePath() {
		return absolutePath;
	}

	@Override
	public void setAbsolutePath(String absolutePath) {
		this.absolutePath = PathUtils.normalize(absolutePath);
		name = new File(this.absolutePath).getName();
	}

	@Override
	public List<INode> getChildren() {
		return children;
	}

	@Override
	public void setChildren(List<INode> children) {
		this.children = children;
	}

	@Override
	public List<Dependency> getDependencies() {
		return dependencies;
	}

	@Override
	public void setDependencies(List<Dependency> dependencies) {
		this.dependencies = dependencies;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Node) {
			Node objCast = (Node) obj;
			return objCast.getAbsolutePath().equals(getAbsolutePath());
		} else
			return true;
	}
}

