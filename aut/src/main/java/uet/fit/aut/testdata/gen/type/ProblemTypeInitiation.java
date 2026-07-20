package uet.fit.aut.testdata.gen.type;

import uet.fit.aut.parser.obj.ExternalVariableNode;
import uet.fit.aut.parser.obj.VariableNode;
import uet.fit.aut.testdata.object.*;
import uet.fit.aut.util.TemplateUtils;
import uet.fit.aut.util.VariableTypeUtils;

/**
 * Khoi tao bien dau vao la kieu co ban
 */
public class ProblemTypeInitiation extends AbstractTypeInitiation {

    private String environmentPath;

    public ProblemTypeInitiation(VariableNode vParent, DataNode nParent) throws Exception {
        super(vParent, nParent);
    }

    public void setEnvironmentPath(String environmentPath) {
        this.environmentPath = environmentPath;
    }

    @Override
    public ValueDataNode execute() throws Exception {
        String type = VariableTypeUtils.getSimpleRealType(vParent);
        type = VariableTypeUtils.deleteReferenceOperator(type);

        ValueDataNode child = null;
        if (VariableTypeUtils.isVoidPointer(type)) {
            child = new VoidPointerDataNode();
        } else if (VariableTypeUtils.isVoid(type)){
            child = new VoidDataNode();
        } else if (VariableTypeUtils.isQT(type) && QTDataNode.ENABLE) {
            child = QTDataNode.fromJson(TemplateUtils.deleteTemplateParameters(type));
        } else
            child = new OtherUnresolvedDataNode();

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
