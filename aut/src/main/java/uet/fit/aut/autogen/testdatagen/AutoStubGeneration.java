package uet.fit.aut.autogen.testdatagen;

import uet.fit.aut.util.SpecialCharacter;
import uet.fit.aut.util.Utils;

import java.util.HashMap;

public class AutoStubGeneration implements IAutoStubGeneration{

    private HashMap<String, Integer> functionToOrdinalNumber;

    public AutoStubGeneration() {
        this.functionToOrdinalNumber = new HashMap<>();
    }

    public HashMap<String, Integer> getFunctionToOrdinalNumber() {
        return functionToOrdinalNumber;
    }

    public String getStubName(String functionName) {
        functionName = Utils.deleteBracesAndParams(functionName);
        Integer ordinalNumber = functionToOrdinalNumber.get(functionName);
        return functionName + SpecialCharacter.UNDERSCORE + FUNCTION_CALL_POSTFIX + ordinalNumber;
    }

    public void increaseOrdinalNumber(String functionName){
        functionName = Utils.deleteBracesAndParams(functionName);
        if (functionToOrdinalNumber.containsKey(functionName))
            functionToOrdinalNumber.put(functionName, functionToOrdinalNumber.get(functionName) + 1);
        else
            functionToOrdinalNumber.put(functionName, 1);
    }
}
