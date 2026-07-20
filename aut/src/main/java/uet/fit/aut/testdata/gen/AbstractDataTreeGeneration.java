package uet.fit.aut.testdata.gen;

import uet.fit.aut.parser.obj.ICommonFunctionNode;
import uet.fit.aut.testdata.object.IDataNode;
import uet.fit.aut.testdata.object.RootDataNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractDataTreeGeneration implements IDataTreeGeneration {

    protected ICommonFunctionNode functionNode;
    protected RootDataNode root = new RootDataNode();
    protected Map<String, String> values = new HashMap<>();

    public int START_VIRTUAL_VARIABLE = 97; //'a'
    public int END_VIRTUAL_VARIABLE = 122; //'z'
    public int currentVirturalIndex = START_VIRTUAL_VARIABLE;
    public List<String> avoidingNames = new ArrayList<>();
    /**
     * Set virtual for nodes in the tree.
     *
     * @param n data node
     */
    public void setVituralName(IDataNode n) {
        if (n == null)
            return;
        else
            n.setVirtualName();

        if (n.getChildren() != null)
            for (IDataNode child : n.getChildren())
                this.setVituralName(child);
    }

    @Override
    public ICommonFunctionNode getFunctionNode() {
        return functionNode;
    }

    @Override
    public void setFunctionNode(ICommonFunctionNode functionNode) {
        this.functionNode = functionNode;
    }

    @Override
    public void setRoot(RootDataNode root) {
        this.root = root;
    }

    @Override
    public RootDataNode getRoot() {
        return root;
    }

    public Map<String, String> getValues() {
        return values;
    }

    public void setValues(Map<String, String> values) {
        this.values = values;
    }
}
