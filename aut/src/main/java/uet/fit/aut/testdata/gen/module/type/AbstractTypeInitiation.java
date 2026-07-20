package uet.fit.aut.testdata.gen.module.type;

import uet.fit.aut.parser.obj.VariableNode;
import uet.fit.aut.testdata.object.DataNode;
import uet.fit.aut.testdata.object.ValueDataNode;

public abstract class AbstractTypeInitiation implements ITypeInitiation {
    protected VariableNode vParent;
    protected DataNode nParent;

//    public AbstractTypeInitiation() {
//
//    }

    public AbstractTypeInitiation(VariableNode vParent, DataNode nParent) throws Exception {
        this.vParent = vParent;
        this.nParent = nParent;
//        execute();
    }

    @Override
    public ValueDataNode execute() throws Exception {
        return null;

    }


}
