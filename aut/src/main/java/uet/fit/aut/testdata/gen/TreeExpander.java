package uet.fit.aut.testdata.gen;


import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.parser.obj.IVariableNode;
import uet.fit.aut.parser.obj.StructureNode;
import uet.fit.aut.parser.obj.VariableNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.testdata.object.ValueDataNode;


/**
 * @author ducanh
 */
public class TreeExpander extends AbstractDataTreeExpander {
    final static Logger logger = LoggerFactory.getLogger(TreeExpander.class);

//    private ICommonFunctionNode functionNode;

    public TreeExpander() {
//        super(false);
    }

//    public TreeExpander(boolean skipTypeResolver) {
//        super(skipTypeResolver);
//    }

    public void expandStructureNodeOnDataTree(ValueDataNode node, String name) throws Exception {
        node.getChildren().clear();

        VariableNode vParent = node.getCorrespondingVar();
        INode correspondingNode = vParent.getCorrespondingNode();

        if (correspondingNode instanceof StructureNode) {
            StructureNode childClass = (StructureNode) correspondingNode;
            for (IVariableNode n : childClass.getPublicAttributes()) {
                if (n.getName().contains(name))
                    generateStructureItem((VariableNode) n, vParent + "." + name, node);
            }
        }else{
            logger.error("Do not handle the case " + correspondingNode.getClass());
        }
    }

//	public ICommonFunctionNode getFunctionNode() {
//		return functionNode;
//	}

//	public void setFunctionNode(ICommonFunctionNode functionNode) {
//		this.functionNode = functionNode;
//	}
}
