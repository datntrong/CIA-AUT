package uet.fit.aut.testdata.gen.module.type;

import uet.fit.aut.logger.AUTLogger;
import uet.fit.aut.parser.obj.ExternalVariableNode;
import uet.fit.aut.parser.obj.VariableNode;
import uet.fit.aut.testdata.object.*;
import uet.fit.aut.util.VariableTypeUtils;

/**
 * Khoi tao bien dau vao la kieu co ban
 */
public class ProblemTypeInitiation extends AbstractTypeInitiation {
    final static AUTLogger logger = AUTLogger.get(ProblemTypeInitiation.class);

    public ProblemTypeInitiation(VariableNode vParent, DataNode nParent) throws Exception {
        super(vParent, nParent);
    }

    @Override
    public ValueDataNode execute() throws Exception {
        String type =vParent.getRealType();
        ValueDataNode child = null;
        if (VariableTypeUtils.isVoidPointer(type)) {
            child = new VoidPointerDataNode();
        } else if (VariableTypeUtils.isVoid(type)){
            child = new VoidDataNode();
        } else
            child = new OtherUnresolvedDataNode();

        child.setParent(nParent);
        child.setRawType(vParent.getRawType());
        child.setRealType(vParent.getRealType());
        child.setName(vParent.getNewType());
        child.setCorrespondingVar(vParent);

        if (vParent instanceof ExternalVariableNode)
            child.setExternel(true);

        nParent.addChild(child);

        return child;
    }


}
