package uet.fit.aut.testdata.gen.module.type;

import uet.fit.aut.logger.AUTLogger;
import uet.fit.aut.parser.obj.ExternalVariableNode;
import uet.fit.aut.parser.obj.VariableNode;
import uet.fit.aut.testdata.object.*;

public class FunctionPointerTypeInitiation extends AbstractTypeInitiation {
    final static AUTLogger logger = AUTLogger.get(FunctionPointerTypeInitiation.class);

    public FunctionPointerTypeInitiation(VariableNode vParent, DataNode nParent) throws Exception {
        super(vParent, nParent);
    }

    @Override
    public ValueDataNode execute() throws Exception {
        FunctionPointerDataNode child = new FunctionPointerDataNode();

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
