package uet.fit.aut.testdata.object;

import uet.fit.aut.util.SpecialCharacter;
import uet.fit.aut.util.TemplateUtils;
import uet.fit.aut.util.VariableTypeUtils;

public class OneDimensionPointerDataNode extends OneDimensionDataNode {

    @Override
    public String getInputForGoogleTest(boolean isDeclared) throws Exception {
        String declaration = "";

        if (isUseUserCode()) {
            if (isPassingVariable() && !isDeclared)
                return SpecialCharacter.EMPTY;
            else
                return getUserCodeContent();
        }

        if(!isPassingVariable() || (isPassingVariable() && isDeclared) ){
            // get type
            String type = VariableTypeUtils.deleteStorageClassesExceptConst(this.getRawType());
            type = VariableTypeUtils.deleteReferenceOperator(type);

            String coreType = type.replaceAll("\\[.*\\]", "");

            if (TemplateUtils.isTemplate(type))
                if (!getChildren().isEmpty()) {
                    IDataNode first = getChildren().get(0);
                    if (first instanceof ValueDataNode)
                        coreType = ((ValueDataNode) first).getRawType();
                }

            if (isExternal() && !isDeclared) {
                coreType = "";
            }

            // get indexes
//        List<String> indexes = Utils.getIndexOfArray(TemplateUtils.deleteTemplateParameters(type));
//        if (indexes.size() > 0) {
            String dimension = "[" + getSize() + "]";

            // generate declaration
            if (this.isAttribute()) {
                declaration += "";
            } else if (this.isPassingVariable()) {
                declaration += String.format("%s %s%s" + SpecialCharacter.END_OF_STATEMENT, coreType,
                        this.getVirtualName(), dimension);
            } else if (isSutExpectedArgument() || isGlobalExpectedValue()) {
                if (!dimension.equals("[-1]")) {
                    declaration += String.format("%s %s%s" + SpecialCharacter.END_OF_STATEMENT, coreType,
                            this.getVirtualName(), dimension);
                }
            } else if (isVoidPointerValue()) {
                declaration += String.format("%s %s%s" + SpecialCharacter.END_OF_STATEMENT, coreType,
                        this.getVirtualName(), dimension);
            }
//        }
        }

        return declaration + SpecialCharacter.LINE_BREAK + super.getInputForGoogleTest(isDeclared);
    }
}
