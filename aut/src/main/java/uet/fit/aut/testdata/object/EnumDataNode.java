package uet.fit.aut.testdata.object;

import uet.fit.aut.parser.obj.EnumNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.testdata.comparable.AssertMethod;
import uet.fit.aut.testdata.comparable.IEqualityComparable;
import uet.fit.aut.testdata.comparable.ValueStatementGenerator;
import uet.fit.aut.util.SpecialCharacter;
import uet.fit.aut.util.VariableTypeUtils;

import java.util.List;

/**
 * Created by DucToan on 27/07/2017
 */
public class EnumDataNode extends StructureDataNode implements IEqualityComparable {
    /**
     * Represent value of variable
     */
    private String value;
    private boolean valueIsSet = false;

    @Override
    public String getInputForGoogleTest(boolean isDeclared) throws Exception {
        String input= "";

        if (isUseUserCode()) {
            if (isPassingVariable() && !isDeclared)
                return SpecialCharacter.EMPTY;
            else
                return super.getInputForGoogleTest(isDeclared);
        }

        if(!isPassingVariable() || (isPassingVariable() && isDeclared) ){
            input = super.getInputForGoogleTest(isDeclared);

            // get type of variable
            String typeVar = VariableTypeUtils.getFullRawType(getCorrespondingVar());
            typeVar = VariableTypeUtils.deleteReferenceOperator(typeVar);
            typeVar = VariableTypeUtils.deleteStorageClassesExceptConst(typeVar);
            //TODO: get isC() from Environment
//            if (Environment.getInstance().isC()) {
//                typeVar = typeVar.replace(SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS, SpecialCharacter.EMPTY);
//
//                if (getCorrespondingType() instanceof EnumNode && !(getCorrespondingType() instanceof EnumTypedefNode)) {
//                    if (!typeVar.startsWith("enum "))
//                        typeVar = "enum " + typeVar;
//                }
//
////            INode correspondingType = getCorrespondingType();
////            if (correspondingType instanceof StructureNode && !((StructureNode) correspondingType).haveTypedef()) {
////                if (!typeVar.startsWith("enum"))
////                    typeVar = "enum " + typeVar;
////            }
//            }

            if (this.getValue() != null) {
                String valueVar = getValue();

                if (!getAllNameEnumItems().contains(valueVar)) {
                    String enumName = getEnumName(valueVar);
                    valueVar = enumName == null ?
                            (typeVar + "(" + valueVar + ")")
                            :
                            (typeVar + SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS + enumName);
                } else {
                        valueVar = typeVar + SpecialCharacter.STRUCTURE_OR_NAMESPACE_ACCESS + valueVar;
                }
                valueVar = VariableTypeUtils.deleteEnumkeyword(valueVar);

                if (isExternal())
                    typeVar = "";

                // generate the statement
                if (this.isPassingVariable()) {
                    input += typeVar + " " + this.getVirtualName() + "=" + valueVar + SpecialCharacter.END_OF_STATEMENT;

                } else if (this.isAttribute()) {
                    input += this.getVirtualName() + "=" + valueVar + SpecialCharacter.END_OF_STATEMENT;

                } else if (this.isArrayElement()) {
                    input += this.getVirtualName() + "=" + valueVar + SpecialCharacter.END_OF_STATEMENT;

                } else if (isSTLListBaseElement()) {
                    input += typeVar + " " + this.getVirtualName() + "=" + valueVar + SpecialCharacter.END_OF_STATEMENT;

                } else if (this.isInConstructor()){
                    input += typeVar + " " + this.getVirtualName() + "=" + valueVar + SpecialCharacter.END_OF_STATEMENT;

                } else {
                    input += typeVar + " " + this.getVirtualName() + "=" + valueVar + SpecialCharacter.END_OF_STATEMENT;
                }

            } else if (isPassingVariable()) {
                input += typeVar + " " + getVirtualName() + SpecialCharacter.END_OF_STATEMENT;
            }
        }

        return input + SpecialCharacter.LINE_BREAK;
    }

    /**
     * Get all name defined in the declaration of enum. For example, enum "Color {
     * RED=10, GREEN=40, BLUE}" -----> "RED", "GREEN", "BLUE"
     *
     * @return all enum items name
     */
    public List<String> getAllNameEnumItems() {
        INode coreType = getCorrespondingVar().getCorrespondingNode();
        if (coreType instanceof EnumNode) {
            return ((EnumNode) coreType).getAllNameEnumItems();
        }
        return null;
    }

    public String getEnumName(String value) {
        INode coreType = getCorrespondingVar().getCorrespondingNode();
        if (coreType instanceof EnumNode) {
            EnumNode enumNode = ((EnumNode) coreType);
            List<String[]> enumItems = enumNode.getAllEnumItems();
            for (String[] enumItem : enumItems) {
                if (enumItem[1].equals(value))
                    return enumItem[0];
            }
        }
        return null;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setValueIsSet(boolean valueIsSet) {
        this.valueIsSet = valueIsSet;
    }

    @Override
    public boolean haveValue() {
        return valueIsSet;
    }

    public boolean isSetValue() {
        return valueIsSet;
    }

    @Override
    public String getAssertion() {
        String expectedName = getVirtualName();
        String actualName = getActualName();

        String assertMethod = getAssertMethod();

        if (assertMethod != null) {
            switch (assertMethod) {
                case AssertMethod.ASSERT_EQUAL:
                    return assertEqual(expectedName, actualName);
                case AssertMethod.ASSERT_NOT_EQUAL:
                    return assertNotEqual(expectedName, actualName);
                case AssertMethod.USER_CODE:
                    return getAssertUserCode().normalize();
            }
        }

        return SpecialCharacter.EMPTY;
    }

    @Override
    public String assertEqual(String expected, String actual) {
        return new ValueStatementGenerator(this).assertEqual(expected, actual);
    }

    @Override
    public String assertNotEqual(String expected, String actual) {
        return new ValueStatementGenerator(this).assertNotEqual(expected, actual);
    }
}
