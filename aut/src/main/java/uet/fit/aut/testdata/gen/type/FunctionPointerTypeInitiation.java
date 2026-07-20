package uet.fit.aut.testdata.gen.type;


import uet.fit.aut.parser.obj.ExternalVariableNode;
import uet.fit.aut.parser.obj.VariableNode;
import uet.fit.aut.testdata.object.DataNode;
import uet.fit.aut.testdata.object.FunctionPointerDataNode;
import uet.fit.aut.testdata.object.ValueDataNode;


public class FunctionPointerTypeInitiation extends AbstractTypeInitiation {
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
            child.setExternal(true);

        nParent.addChild(child);

        return child;
    }


}
