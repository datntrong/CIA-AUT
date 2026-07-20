package uet.fit.aut.parser.dependency;

import uet.fit.aut.logger.IdMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.parser.obj.IFunctionNode;
import uet.fit.aut.parser.obj.IVariableNode;
import uet.fit.aut.parser.vardect.RelatedExternalVariableDetecter;

import java.util.List;

public class GlobalVariableDependencyGeneration extends AbstractDependencyGeneration<IFunctionNode> {

    private final static Logger logger = LoggerFactory.getLogger(GlobalVariableDependencyGeneration.class);

    public void dependencyGeneration(IFunctionNode root) {
        if (!root.isGlobalVariableDependencyState()) {
            List<IVariableNode> externalVars = new RelatedExternalVariableDetecter(root).findVariables();
            for (IVariableNode referredNode : externalVars) {
                GlobalVariableDependency d = new GlobalVariableDependency(root, referredNode);
                if (!root.getDependencies().contains(d)
                        && !referredNode.getDependencies().contains(d)) {
                    root.getDependencies().add(d);
                    referredNode.getDependencies().add(d);

                    logger.debug("Found a global dependency: " + d);
                }
            }
            root.setGlobalVariableDependencyState(true);
        } else {
            logger.debug(IdMapping.getInstance().getOrCreate(root.getAbsolutePath()) + " is analyzed global dependency before");
        }
    }
}
