package uet.fit.aut.testdata.object;

import uet.fit.aut.util.SpecialCharacter;
import uet.fit.aut.util.TemplateUtils;
import uet.fit.aut.util.VariableTypeUtils;

public class OneDimensionStructureDataNode extends OneDimensionDataNode {

    @Override
    public String getInputForGoogleTest(boolean isDeclared) throws Exception {
        String input = "";

        if (isUseUserCode()) {
            if (isPassingVariable() && !isDeclared)
                return SpecialCharacter.EMPTY;
            else
                return getUserCodeContent();
        }

        if(!isPassingVariable() || (isPassingVariable() && isDeclared) ){
            String declarationType = VariableTypeUtils.deleteStorageClassesExceptConst(this.getRawType());
            declarationType = VariableTypeUtils.deleteReferenceOperator(declarationType);

            String coreType = VariableTypeUtils
                    .deleteStorageClasses(declarationType).replaceAll("\\[.*\\]", "");

            if (TemplateUtils.isTemplate(declarationType))
                if (!getChildren().isEmpty()) {
                    IDataNode first = getChildren().get(0);
                    if (first instanceof ValueDataNode)
                        coreType = ((ValueDataNode) first).getRawType();
                }

            if (isExternal())
                coreType = "";

            int size = getSize();

            if (this.isPassingVariable()){
                input += coreType + " " + getVirtualName() + "[" + size + "]" + SpecialCharacter.END_OF_STATEMENT;

            } else if (this.isAttribute()) {
                input += "";
            } else if (isSutExpectedArgument() || isGlobalExpectedValue()) {
                if (size > 0) {
                    input += coreType + " " + getVirtualName() + "[" + size + "]" + SpecialCharacter.END_OF_STATEMENT;
                }
            } else if (isVoidPointerValue()) {
                input += coreType + " " + getVirtualName() + "[" + size + "]" + SpecialCharacter.END_OF_STATEMENT;
            }
        }

        return input + SpecialCharacter.LINE_BREAK + super.getInputForGoogleTest(isDeclared);
    }
}
