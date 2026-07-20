package uet.fit.aut.testdata.object;

import uet.fit.aut.util.SpecialCharacter;
import uet.fit.aut.util.Utils;
import uet.fit.aut.util.VariableTypeUtils;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class OneDimensionCharacterDataNode extends OneDimensionDataNode {

    @Override
    public String getInputForGoogleTest(boolean isDeclared) throws Exception {
        String declaration = "";

        if (isUseUserCode()) {
            if (isPassingVariable() && !isDeclared)
                return SpecialCharacter.EMPTY;
            else
                return getUserCodeContent();
        }

        if(!isPassingVariable() || (isPassingVariable() && isDeclared) ) {
            // get type
            String type = VariableTypeUtils.deleteStorageClassesExceptConst(this.getRawType());
            type = VariableTypeUtils.deleteReferenceOperator(type);
            String coreType = type.replaceAll("\\[.*\\]", "");
            if (isExternal()) {
                coreType = "";
            }

            // get indexes
            List<String> indexes = Utils.getIndexOfArray(type);
            if (indexes.size() > 0) {
                StringBuilder dimension = new StringBuilder();
                for (String index : indexes) {
                    if (index.length() == 0)
                        dimension.append(Utils.asIndex(this.getSize())); // set the configured size of array
                    else
                        dimension.append(Utils.asIndex(index));
                }

                // generate declaration
                if (this.isAttribute()) {
                    declaration += "";
                } else if (this.isPassingVariable()) {
                    declaration += String.format("%s %s%s" + SpecialCharacter.END_OF_STATEMENT, coreType,
                            this.getVirtualName(), dimension.toString());
                }  else if (isSutExpectedArgument() || isGlobalExpectedValue()) {
//                    String firstIndex = indexes.get(0);
//                    if (!firstIndex.equals("-1") && !firstIndex.isEmpty()) {
                        declaration += String.format("%s %s%s" + SpecialCharacter.END_OF_STATEMENT, coreType,
                                this.getVirtualName(), dimension.toString());
//                    }
                } else if (isVoidPointerValue()) {
                    declaration += String.format("%s %s%s" + SpecialCharacter.END_OF_STATEMENT, coreType,
                            this.getVirtualName(), dimension.toString());
                }
            }
        }

        return declaration + SpecialCharacter.LINE_BREAK + super.getInputForGoogleTest(isDeclared);
    }
}
