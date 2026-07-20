package uet.fit.aut.testdata.gen.type;


import uet.fit.aut.parser.obj.ExternalVariableNode;
import uet.fit.aut.parser.obj.VariableNode;
import uet.fit.aut.testdata.object.DataNode;
import uet.fit.aut.testdata.object.EnumDataNode;
import uet.fit.aut.testdata.object.ValueDataNode;

/**
 * Khoi tao bien dau vao la kieu Enum
 */
public class EnumTypeInitiation extends AbstractTypeInitiation {
    public EnumTypeInitiation(VariableNode vParent, DataNode nParent) throws Exception {
        super(vParent, nParent);
    }

    @Override
    public ValueDataNode execute() throws Exception {
        EnumDataNode child = new EnumDataNode();

        child.setParent(nParent);
        child.setName(vParent.getNewType());
        child.setRawType(vParent.getRawType());
        child.setRealType(vParent.getRealType());
        child.setCorrespondingVar(vParent);
        if (vParent instanceof ExternalVariableNode)
            child.setExternal(true);
        nParent.addChild(child);
        return  child;
    }
}
