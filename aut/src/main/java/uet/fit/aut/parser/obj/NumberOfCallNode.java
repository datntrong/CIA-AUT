package uet.fit.aut.parser.obj;

import uet.fit.aut.testdata.comparable.*;
import uet.fit.aut.testdata.object.ValueDataNode;
import uet.fit.aut.util.SpecialCharacter;

import java.util.ArrayList;
import java.util.List;

public class NumberOfCallNode extends ValueDataNode {

    private String value;

    public NumberOfCallNode() {
        super.setName("Number of calls");
        super.setRealType("int");
        super.setRawType("int");
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean haveValue() {
        return value != null && !value.isEmpty();
    }

    public void setValue(int value) {
        this.value = value + "";
    }

    @Override
    public String[] getAllSupportedAssertMethod() {
        List<String> supportedMethod = new ArrayList<>();
        supportedMethod.add(SpecialCharacter.EMPTY);
        supportedMethod.add(AssertMethod.ASSERT_EQUAL);
        supportedMethod.add(AssertMethod.USER_CODE);
        return supportedMethod.toArray(new String[0]);
    }
}
