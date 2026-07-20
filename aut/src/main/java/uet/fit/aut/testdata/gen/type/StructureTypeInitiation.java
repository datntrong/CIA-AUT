package uet.fit.aut.testdata.gen.type;


import uet.fit.aut.parser.obj.*;
import uet.fit.aut.testdata.gen.TreeExpander;
import uet.fit.aut.testdata.object.*;
import uet.fit.aut.util.TemplateUtils;
import uet.fit.aut.util.VariableTypeUtils;

/**
 * Khoi tao bien truyen vao la kieu structure
 */
public class StructureTypeInitiation extends AbstractTypeInitiation {

    public StructureTypeInitiation(VariableNode vParent, DataNode nParent) throws Exception {
        super(vParent, nParent);
    }

    @Override
    public ValueDataNode execute() throws Exception {
        INode correspondingNode = vParent.getCorrespondingNode();
//        if (correspondingNode == null)
//            correspondingNode = ResolveCoreTypeHelper.resolve(vParent);

        if (correspondingNode == null && VariableTypeUtils.isStdInt(vParent.getRealType()))
            return new BasicTypeInitiation(vParent, nParent).execute();

        ValueDataNode child = null;

        String type = vParent.getRawType();

        String realType = vParent.getRealType();
        realType = VariableTypeUtils.removeRedundantKeyword(realType);
        if (VariableTypeUtils.isPointer(realType)) {
            return new PointerTypeInitiation(vParent, nParent).execute();
        }

        String simpleRealType = VariableTypeUtils.getSimpleRealType(vParent);
        simpleRealType = VariableTypeUtils.deleteReferenceOperator(simpleRealType);

        if (correspondingNode instanceof StructNode)
            child = new StructDataNode();
        else if (correspondingNode instanceof ClassNode)
            child = new ClassDataNode();
        else if (correspondingNode instanceof UnionNode)
            child = new UnionDataNode();
        else if (correspondingNode instanceof EnumNode)
            return new EnumTypeInitiation(vParent, nParent).execute();
        else if (VariableTypeUtils.isNullPtr(type)) {
            child = new NullPointerDataNode();
        } else if (correspondingNode instanceof STLTypeNode) {
            return new STLTypeInitiation(vParent, nParent, ((STLTypeNode) correspondingNode).getType()).execute();
        } else if (correspondingNode instanceof AvailableTypeNode) {
            return new BasicTypeInitiation(vParent, nParent).execute();
        }  else  if (VariableTypeUtils.isVoidPointer(type)){
            child = new VoidPointerDataNode();
        } else if (VariableTypeUtils.isVoid(type)) {
            child = new VoidDataNode();
        } else if (VariableTypeUtils.isQT(simpleRealType) && QTDataNode.ENABLE) {

            String childName = TemplateUtils.deleteTemplateParameters(simpleRealType);
            child = QTDataNode.fromJson(childName);

            if (childName.contains("QSharedPointer")) {

                IDataNode parent = nParent.getParent();
                while (!(parent instanceof QTDataNode)) {
                    parent = parent.getParent();
                }

                child.setParent(nParent);
                child.setName(vParent.getNewType());

                child.setRawType(((QTDataNode) parent).getRawType());
                child.setRealType(((QTDataNode) parent).getRealType());
//                child.setRawType(vParent.getRawType());
////        child.setType(vParent.getFullType());
//                child.setRealType(vParent.getRealType());
                child.setCorrespondingVar(vParent);

                if (correspondingNode instanceof StructNode) {
                    (new TreeExpander()).expandTree(child);
                }

                if (vParent instanceof ExternalVariableNode)
                    child.setExternal(true);

                nParent.addChild(child);
                return child;
            }
        } else {
            child = new OtherUnresolvedDataNode();
        }

        child.setParent(nParent);
        child.setName(vParent.getNewType());
        child.setRawType(vParent.getRawType());
//        child.setType(vParent.getFullType());
        child.setRealType(vParent.getRealType());
        child.setCorrespondingVar(vParent);

        if (correspondingNode instanceof StructNode) {
            (new TreeExpander()).expandTree(child);
        }

        if (vParent instanceof ExternalVariableNode)
            child.setExternal(true);

        nParent.addChild(child);
        return child;
    }

}
