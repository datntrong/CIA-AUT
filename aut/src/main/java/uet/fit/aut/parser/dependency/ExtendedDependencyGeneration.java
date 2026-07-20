package uet.fit.aut.parser.dependency;

import uet.fit.aut.parser.obj.AvailableTypeNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.parser.obj.StructOrClassNode;
import uet.fit.aut.parser.obj.VariableNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ExtendedDependencyGeneration extends AbstractDependencyGeneration<StructOrClassNode> {

    private final static Logger logger = LoggerFactory.getLogger(ExtendedDependencyGeneration.class);

    public void dependencyGeneration(StructOrClassNode n) {
        // if the current node is has ability to inherit and it is never analyzed extended dependency before
        List<String> extendClassNames = n.getExtendedNames();
        if (extendClassNames != null)
            for (String extendClassName : extendClassNames) {
                /*
                 * Create temporary variable
                 */
                VariableNode v = new VariableNode();
                v.setCoreType(extendClassName);
                v.setRawType(extendClassName);
                v.setName(extendClassName);
                v.setParent(n);
                /*
                 * Find type of temporary variable
                 */
                CTypeDependencyGeneration typeGen;
                try {
                    typeGen = new CTypeDependencyGeneration();
                    typeGen.setAddToTreeAutomatically(false);// because the variable node is fake, we can not update the tree
                    // if we found any type dependencies
                    typeGen.dependencyGeneration(v);

                    INode correspondingNode = typeGen.getCorrespondingNode();
                    if (correspondingNode != null && !(correspondingNode instanceof AvailableTypeNode)) {
                        ExtendDependency d = new ExtendDependency(n, correspondingNode);

                        if (!n.getDependencies().contains(d)) {
                            n.getDependencies().add(d);
                            correspondingNode.getDependencies().add(d);
                            logger.debug("Found an extended dependency: " + d);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
    }
}
