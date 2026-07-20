package uet.fit.aut.testdata.object.stl;

import uet.fit.aut.testdata.object.IDataNode;
import uet.fit.aut.util.SpecialCharacter;
import uet.fit.aut.util.VariableTypeUtils;

public class PairDataNode extends STLDataNode {
    public String getFirstType() {
        if (getArguments() != null && getArguments().size() >= 2) {
            return getArguments().get(0);
        }

        return null;
    }

    public String getSecondType() {
        if (getArguments() != null && getArguments().size() >= 2) {
            return getArguments().get(1);
        }

        return null;
    }

    @Override
    public String getInputForGoogleTest(boolean isDeclared) throws Exception {
        String input = "";
        if(!isPassingVariable() || (isPassingVariable() && isDeclared) ){
            // get type of variable
            String typeVar = getRawType();
            typeVar = VariableTypeUtils.deleteReferenceOperator(typeVar);
            typeVar = VariableTypeUtils.deleteStorageClassesExceptConst(typeVar);
            typeVar = VariableTypeUtils.deleteStorageClassesExceptConst(typeVar);
            typeVar = VariableTypeUtils.deleteStructKeyword(typeVar);
            typeVar = VariableTypeUtils.deleteUnionKeyword(typeVar);

            if (isExternal())
                typeVar = "";

            // generate the statement
            if (this.isPassingVariable()) {
                input += typeVar + " " + this.getVirtualName() + SpecialCharacter.END_OF_STATEMENT;

            } else if (isSTLListBaseElement()) {
                input += typeVar + " " + this.getVirtualName() + SpecialCharacter.END_OF_STATEMENT;

            } else if (this.isInConstructor()) {
                input += typeVar + " " + this.getVirtualName() + SpecialCharacter.END_OF_STATEMENT;
            } else if (isSutExpectedArgument() || isGlobalExpectedValue()) {
                input += typeVar + " " + this.getVirtualName() + SpecialCharacter.END_OF_STATEMENT;
            }
        }

        return  input + SpecialCharacter.LINE_BREAK + super.getInputForGoogleTest(isDeclared);
    }
}
