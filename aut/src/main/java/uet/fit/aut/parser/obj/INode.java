package uet.fit.aut.parser.obj;

import uet.fit.aut.parser.dependency.Dependency;

import java.util.List;

/**
 * Interface represents a node in a tree, e.g., structure tree
 */
public interface INode {

    INode clone();

    String getAbsolutePath();

    void setAbsolutePath(String absolutePath);

    List<INode> getChildren();

    void setChildren(List<INode> children);

    List<Dependency> getDependencies();

    void setDependencies(List<Dependency> dependencies);

    String getNewType();

    INode getParent();

    void setParent(INode parent);

    String getName();

    void setName(String name);
}
