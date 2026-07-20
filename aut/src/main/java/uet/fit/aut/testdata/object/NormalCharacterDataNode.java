package uet.fit.aut.testdata.object;

import uet.fit.aut.util.SpecialCharacter;
import uet.fit.aut.util.VariableTypeUtils;

import java.util.HashMap;
import java.util.Map;

public class NormalCharacterDataNode extends NormalDataNode {
    public static String VISIBLE_CHARACTER_PREFIX = "'";

    public static String RULE = "\nRules:" +
            "\n\t- For visible character, type with `'`. Example: 'a', 'b', 'c'" +
            "\n\t- For ascii, its value. Example: 100, 200, 300" +
            "\n\t- For special character such as tab or line break, type '\\t' or '\\n'";

    @Override
    public String getInputForGoogleTest(boolean isDeclared) throws Exception {
        String input = SpecialCharacter.EMPTY;

        if (isUseUserCode()) {
            if (isPassingVariable() && !isDeclared)
                return SpecialCharacter.EMPTY;
            else
                return getUserCodeContent();
        }

        if(!isPassingVariable() || (isPassingVariable() && isDeclared) ) {
            input = super.getInputForGoogleTest(isDeclared);

            // get type of variable
            String typeVar = VariableTypeUtils.deleteStorageClassesExceptConst(this.getRawType());
            typeVar = VariableTypeUtils.deleteReferenceOperator(typeVar);
            typeVar = VariableTypeUtils.deleteStorageClassesExceptConst(typeVar);
            typeVar = VariableTypeUtils.deleteStructKeyword(typeVar);
            typeVar = VariableTypeUtils.deleteUnionKeyword(typeVar);

            if (this.getValue() != null) {
                if (isExternal())
                    typeVar = "";

                String valueVar = getValue();
//            String valueVar = "";
//            if (getValue().matches(OCTAL_NUMBER))
//                valueVar = getValue().substring(1);
//
//            else if (getValue().matches(SPECIAL_CHAR)){
//                valueVar = NormalDataNode.CHARACTER_QUOTE + "\\" + getValue() + NormalDataNode.CHARACTER_QUOTE;
//
//            } else if (getValue().equals("\\")) {
//                valueVar = 92 + "";
//
//            } else if (getValue().equals("\"")) {
//                valueVar = 34 + "";
//
//            }  else if (getValue().equals("'")) {
//                valueVar = 39 + "";
//
//            } else if (getValue().length() == 1)
//                // character
//                valueVar = NormalDataNode.CHARACTER_QUOTE + getValue() + NormalDataNode.CHARACTER_QUOTE;

                // generate the statement
                if (this.isPassingVariable()) {
                    input += typeVar + " " + this.getVirtualName() + "=" + valueVar + SpecialCharacter.END_OF_STATEMENT;

                } else if (this.isAttribute()) {
                    input += this.getVirtualName() + "=" + valueVar + SpecialCharacter.END_OF_STATEMENT;

                } else if (this.isArrayElement()) {
                    input += this.getVirtualName() + "=" + valueVar + SpecialCharacter.END_OF_STATEMENT;

                } else if (isSTLListBaseElement()) {
                    input += typeVar + " " + this.getVirtualName() + "=" + valueVar + SpecialCharacter.END_OF_STATEMENT;

                } else if (this.isInConstructor()) {
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

//    @Override
//    public String getAssertionForGoogleTest(String method, String source, String target) throws Exception {
//        String assertion = "";//super.getAssertionForGoogleTest();
//
//        if (getValue() != null || getVituralName().equals(IGTestConstant.EXPECTED_OUTPUT)) {
//            String actualOutputName = getVituralName().replace(source, target);
//
//            if (Environment.getInstance().isC()){
//                assertion += String.format("%s(%s, %s );", method, getVituralName(), actualOutputName);
//                assertion += getExportExeResultStm(actualOutputName, getVituralName());
//            } else {
//                assertion += method + "(" + getVituralName() + "," + actualOutputName + ")" + IGTestConstant.LOG_FUNCTION_CALLS;
//            }
//        }
//
//        return assertion;
//    }

    public static Map<String, String> getCharacterToACSIIMapping() {
        Map<String, String> mapping = new HashMap<>();
        for (long ascii = 33; ascii <= 126; ascii++)
            mapping.put((char) ascii + "", ascii +"");

        mapping.put("\\t", "9");
        mapping.put("\\n", "10");
        mapping.put("\\r", "11");
        mapping.put(" ", "32");
        return mapping;
    }

    public static String toASCII(String character) throws Exception {
        if (character.startsWith(VISIBLE_CHARACTER_PREFIX)) {
            character = character.substring(1, character.length() - 1);
            Map<String, String> mapping = getCharacterToACSIIMapping();
            return mapping.get(character);
        } else
            return Long.parseLong(character) + "";
    }
}
