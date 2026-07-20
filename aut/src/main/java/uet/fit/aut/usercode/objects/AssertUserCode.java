package uet.fit.aut.usercode.objects;

import uet.fit.aut.testdata.comparable.AssertUserCodeMapping;

public class AssertUserCode extends UsedParameterUserCode {

    public String normalize() {
        try {
            return AssertUserCodeMapping.convert(getContent());
        } catch (Exception ex) {
            return getContent();
        }
    }
}
