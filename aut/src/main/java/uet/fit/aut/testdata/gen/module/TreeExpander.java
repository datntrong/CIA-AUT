package uet.fit.aut.testdata.gen.module;

import uet.fit.aut.logger.AUTLogger;
import uet.fit.aut.parser.obj.*;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.testdata.object.*;

/**
 * @author ducanh
 */
public class TreeExpander extends AbstractDataTreeExpander {
    final static AUTLogger logger = AUTLogger.get(TreeExpander.class);

    public TreeExpander() {

    }

    public void expandStructureNodeOnDataTree(ValueDataNode node, String name) throws Exception {
        node.getChildren().clear();
//        boolean isExist = node.getChildren().stream()
//                .anyMatch(n -> n.getName().equals(name));
//
//        if (!isExist) {
            VariableNode vParent = node.getCorrespondingVar();
            INode correspondingNode = vParent.resolveCoreType();

            if (correspondingNode instanceof StructureNode) {
                StructureNode childClass = (StructureNode) correspondingNode;
                for (IVariableNode n : childClass.getPublicAttributes()) {
                    if (n.getName().contains(name))
                        generateStructureItem((VariableNode) n, vParent + "." + name, node);
                }
            } else {
                logger.error("Do not handle the case " + correspondingNode.getClass());
            }
//        }
    }
}
