package uet.fit.aut.testdata.gen;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.parser.obj.VariableNode;
import uet.fit.aut.testdata.gen.type.*;
import uet.fit.aut.testdata.object.DataNode;
import uet.fit.aut.testdata.object.ValueDataNode;
import uet.fit.aut.util.TemplateUtils;
import uet.fit.aut.util.VariableTypeUtils;

/**
 * Táº¡o cÃ¢y khá»Ÿi Ä‘áº§u dá»±a trÃªn arguments vÃ  external variable Táº¥t cáº£ má»�i biáº¿n
 * truyá»�n vÃ o hÃ m thuá»™c ba loáº¡i:
 * <p>
 * + Biáº¿n cÆ¡ báº£n: Ä�Æ°á»£c sinh giÃ¡ trá»‹ ngáº«u nhiÃªn
 * <p>
 * + Biáº¿n máº£ng: máº·c Ä‘á»‹nh sá»‘ pháº§n tá»­ lÃ  0
 * <p>
 * + Biáº¿n con trá»�: máº·c Ä‘á»‹nh gÃ­a trá»‹ lÃ  null
 *
 * @author ducanh
 */
public class InitialTreeGen {
    protected final static Logger logger = LoggerFactory.getLogger(InitialTreeGen.class);

    public ValueDataNode genInitialTree(VariableNode vCurrentChild, DataNode nCurrentParent) throws Exception {
        String realType = VariableTypeUtils.getSimpleRealType(vCurrentChild);
        realType = VariableTypeUtils.deleteReferenceOperator(realType);

        // Step: Check the type
        ITypeInitiation typeInitiation = null;

        if (VariableTypeUtils.isVoid(realType)) {
            logger.error("Do not support type parameters for void function");

        } else if (TemplateUtils.isTemplateClass(realType)) {
            typeInitiation = new TemplateClassTypeInitiation(vCurrentChild, nCurrentParent);

        } else if (VariableTypeUtils.isBasic(realType)) {
            typeInitiation = new BasicTypeInitiation(vCurrentChild, nCurrentParent);

        } else if (VariableTypeUtils.isMultipleDimension(realType)) {
            typeInitiation = new MultipleDimensionTypeInitiation(vCurrentChild, nCurrentParent);

        } else if (VariableTypeUtils.isOneDimension(realType)) {
            typeInitiation = new OneDimensionTypeInitiation(vCurrentChild, nCurrentParent);

        } else if (VariableTypeUtils.isFunctionPointer(realType)) {
            typeInitiation = new FunctionPointerTypeInitiation(vCurrentChild, nCurrentParent);

        } else if (VariableTypeUtils.isPointer(realType)) {
            typeInitiation = new PointerTypeInitiation(vCurrentChild, nCurrentParent);

        } else if (VariableTypeUtils.isStructureSimple(realType)) {
            typeInitiation = new StructureTypeInitiation(vCurrentChild, nCurrentParent);

        } else  {
            logger.error("Can not handle " + vCurrentChild.toString());
            typeInitiation = new ProblemTypeInitiation(vCurrentChild, nCurrentParent);
        }

        if (typeInitiation == null)
            return null;
        else
            return typeInitiation.execute();
    }
}
