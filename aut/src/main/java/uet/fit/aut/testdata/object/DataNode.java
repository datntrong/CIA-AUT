package uet.fit.aut.testdata.object;

import uet.fit.aut.util.NodeType;
import uet.fit.aut.util.SpecialCharacter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represent a variable node in the <b>variable tree</b>, example: class,
 * struct, array item, etc.
 *
 * @author DucAnh
 */
public abstract class DataNode implements IDataNode {

    /**
     * Represent the fixed name of variable
     * <p>
     * 1. Case 1: The node corresponding to array item. Ex: [0], [1],[0][1], v.v.
     * <p>
     * 2. Case 2: The node represents pointer/array/class/struct/structure/etc.: Ex:
     * sv, x, y, v.v.
     */
    protected String name = "";

    /**
     * The virtual name of node. The name can be changed :D
     */
    protected String virtualName = null;

    /**
     * The parent node of the current node
     */
    protected IDataNode parent = null;

    /**
     * The children of the current node
     */
    protected List<IDataNode> children = new ArrayList<>();

    @Override
    public void addChild(IDataNode newChild) {
        this.children.add(newChild);
    }

    @Override
    public List<IDataNode> getChildren() {
        return this.children;
    }

    @Override
    public void setChildren(List<IDataNode> children) {
        this.children = children;
    }

    @Override
    public Set<String> getAdditionalSources() {
        Set<String> output = new HashSet<>();
        try {
            for (IDataNode child : getChildren()) {
                try {
                    output.addAll(child.getAdditionalSources());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            return output;
        } catch (Exception e) {
            return output;
        }
    }

    @Override
    public String getInputForGoogleTest(boolean isDeclared) throws Exception {
        String output = "";
        boolean explanationEnabled = true;
        if (!isDeclared) {
            if (this.getChildren() != null)
                for (IDataNode child : this.getChildren()) {
                    if (explanationEnabled) {
                        output += SpecialCharacter.LINE_BREAK + "/* " + child.getClass().getSimpleName() + " " + child.getName() + " */" + SpecialCharacter.LINE_BREAK;
                    }
                    output += child.getInputForGoogleTest(isDeclared) + SpecialCharacter.LINE_BREAK;
                }
        }

        if (explanationEnabled) {
            output = output.replace(SpecialCharacter.LINE_BREAK + SpecialCharacter.LINE_BREAK, SpecialCharacter.LINE_BREAK);
            output = output.replace(SpecialCharacter.LINE_BREAK + SpecialCharacter.LINE_BREAK, SpecialCharacter.LINE_BREAK);
        } else {
            //output = output.replace(SpecialCharacter.LINE_BREAK, "");
            output = output.replace(SpecialCharacter.LINE_BREAK, ""); // note: the comment in test case script must be put in '/*' and '*/', not '//'
            output = output.replace(";;", ";");
        }
        return output + SpecialCharacter.LINE_BREAK;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public IDataNode getParent() {
        return this.parent;
    }

    @Override
    public void setParent(IDataNode parent) {
        this.parent = parent;
    }

    @Override
    public final String getVirtualName() {
        if (virtualName == null)
            setVirtualName();

        return virtualName;
    }

    @Override
    public void setVirtualName(String virtualName) {
        this.virtualName = virtualName;
    }

    @Override
    public String toString() {
        return this.getName() + "\n";
    }

    @Override
    public IDataNode getRoot() {
        if (this instanceof RootDataNode)
            return this;
        else {
            IDataNode currentNode = this;
            while (currentNode != null && currentNode.getParent() != null
                    && !(currentNode.getParent() instanceof RootDataNode)) {
                currentNode = currentNode.getParent();
            }
            return currentNode.getParent();
        }
    }

    @Override
    public UnitNode getUnit() {
        if (this instanceof UnitNode)
            return (UnitNode) this;
        else {
            IDataNode currentNode = this;
            while (currentNode != null && currentNode.getParent() != null
                    && !(currentNode.getParent() instanceof UnitNode)) {
                currentNode = currentNode.getParent();
            }
            return (UnitNode) currentNode.getParent();
        }
    }

    public void setVirtualName() {
        if (this.virtualName != null)
            return;

        setVirtualName(NON_VALUE);
    }

    protected static final String NON_VALUE = "NON_VALUE";

    @Override
    public String getPathFromRoot() {
        if (this instanceof RootDataNode && ((RootDataNode) this).getLevel() == NodeType.ROOT)
            return getName();

        return parent.getPathFromRoot() + File.separator + getName();
    }

    public String getDisplayName() {
        return name;
    }

    // get the ROOT node
    public RootDataNode getTestCaseRoot() {
        IDataNode parent = this;

        while (parent != null) {
            if (parent instanceof RootDataNode && ((RootDataNode) parent).getLevel() == NodeType.ROOT) {
                return (RootDataNode) parent;
            } else
                parent = parent.getParent();
        }

        return null;
    }
}
