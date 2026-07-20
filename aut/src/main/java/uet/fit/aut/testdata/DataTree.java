package uet.fit.aut.testdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.parser.funcdetail.IFunctionDetailTree;
import uet.fit.aut.parser.obj.ICommonFunctionNode;
import uet.fit.aut.testdata.gen.DataTreeGeneration;
import uet.fit.aut.testdata.gen.TreeExpander;
import uet.fit.aut.testdata.object.DataNode;
import uet.fit.aut.testdata.object.IDataNode;
import uet.fit.aut.testdata.object.RootDataNode;
import uet.fit.aut.testdata.object.ValueDataNode;
import uet.fit.aut.util.NodeType;

public class DataTree implements IDataTree {
    final static Logger logger = LoggerFactory.getLogger(DataTree.class);
    /**
     * The unit under test
     */
    private ICommonFunctionNode functionNode;

    /**
     * The root of the test data tree
     */
    private RootDataNode root = new RootDataNode();

//    /**
//     * The function detail tree needed to build test data tree
//     */
//    private IFunctionDetailTree functionTree;

    private TreeExpander expander = new TreeExpander();

    public DataTree() {

    }

    public DataTree(IFunctionDetailTree functionTree) {
//        this.functionTree = functionTree;
        try {
            logger.debug("Generate Data Tree");
            new DataTreeGeneration(this, functionTree).generateTree();
        } catch (Exception e) {
             e.printStackTrace();
        }
    }

    @Override
    public void expand(ValueDataNode node) throws Exception {
        expander.expandTree(node);
    }

    @Override
    public void expand(ValueDataNode node, String name) throws Exception {
        expander.expandStructureNodeOnDataTree(node, name);
    }

    @Override
    public ICommonFunctionNode getFunctionNode() {
        return functionNode;
    }

    public void setFunctionNode(ICommonFunctionNode fn) {
        this.functionNode = fn;
    }

    public void setRoot(RootDataNode root) {
        this.root = root;
    }

    @Override
    public RootDataNode getRoot() {
        return root;
    }

    @Override
    public RootDataNode getSubTreeRoot(NodeType level) {
        for (IDataNode inode : root.getChildren()) {
            DataNode node = (DataNode) inode;
            if (node instanceof RootDataNode && ((RootDataNode) node).getLevel() == level)
                return (RootDataNode) node;
        }
        return null;
    }

    @Override
    public RootDataNode getSubTreeRoot(IDataNode node) {
        return (RootDataNode) findSubTreeRoot(node);
    }

    private IDataNode findSubTreeRoot(IDataNode node) {
        if (node instanceof RootDataNode) {
            return node;
        } else {
            return findSubTreeRoot(node.getParent());
        }
    }
}
