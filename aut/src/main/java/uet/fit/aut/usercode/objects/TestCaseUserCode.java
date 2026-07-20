package uet.fit.aut.usercode.objects;

import uet.fit.aut.testdata.comparable.AssertUserCodeMapping;

public class TestCaseUserCode extends AbstractUserCode {
    private String setUpContent;
    private String tearDownContent;

    public void setSetUpContent(String setUpContent) {
        this.setUpContent = setUpContent;
    }

    public void setTearDownContent(String tearDownContent) {
        this.tearDownContent = tearDownContent;
    }

    public String getSetUpContent() {
        try {
            return AssertUserCodeMapping.convert(setUpContent);
        } catch (Exception ex) {
            return setUpContent;
        }
    }
    public String getTearDownContent() {
        try {
            return AssertUserCodeMapping.convert(tearDownContent);
        } catch (Exception ex) {
            return tearDownContent;
        }
    }
}
