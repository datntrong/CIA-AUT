package uet.fit.aut.testdata.object.stl;

import uet.fit.aut.testdata.object.IDataNode;
import uet.fit.aut.util.SpecialCharacter;
import uet.fit.aut.util.VariableTypeUtils;

public class DefaultDeleteDataNode extends STLDataNode {

    @Override
    public boolean haveValue() {
        return false;
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
