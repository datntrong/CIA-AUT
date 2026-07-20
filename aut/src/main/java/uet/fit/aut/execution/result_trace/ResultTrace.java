package uet.fit.aut.execution.result_trace;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import uet.fit.aut.testcase.ITestCase;
import uet.fit.aut.util.SpecialCharacter;
import uet.fit.aut.util.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ResultTrace extends AbstractResultTrace {

    private String message;

    private String userCode;

    private String actualName, expectedName;

    private String actualVal, expectedVal;

    private String tag;

    @Override
    public String getExpected() {
        return expectedVal;
    }

    @Override
    public String getExpectedName() {
        return expectedName;
    }

    @Override
    public String getActualName() {
        return actualName;
    }

    @Override
    public String getActual() {
        return actualVal;
    }

    @Override
    public String getTag() {
        return tag;
    }

    @Override
    public String getUserCode() {
        return userCode;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static List<IResultTrace> load(ITestCase testCase) {
        List<IResultTrace> list = new ArrayList<>();

        String path = testCase.getExecutionResultTrace();

        if (!new File(path).exists())
            return null;

        String content = Utils.readFileContent(path);

        if (!content.trim().isEmpty()) {
            JsonArray jsonArray;
            try {
                jsonArray = JsonParser.parseString(content).getAsJsonArray();
            } catch (JsonSyntaxException e) {
                content = refactorResultTrace(testCase);
                jsonArray = JsonParser.parseString(content).getAsJsonArray();
            }

            for (JsonElement jsonElement : jsonArray) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                ResultTrace failure = new ResultTrace();

                String tag = jsonObject.get("tag").getAsString();
                failure.setTag(tag);

                String actualName = jsonObject.get("actualName").getAsString();
                failure.setActualName(actualName);

                String actualVal = jsonObject.get("actualVal").getAsString();
                failure.setActualVal(actualVal);

                String expectedName = jsonObject.get("expectedName").getAsString();
                failure.setExpectedName(expectedName);

                String expectedVal = jsonObject.get("expectedVal").getAsString();
                failure.setExpectedVal(expectedVal);

                if (jsonObject.get("userCode") != null) {
                    String userCode = jsonObject.get("userCode").getAsString();
                    failure.setUserCode(userCode);
                }

                failure.setMessage(jsonObject.toString());
                if (!list.contains(failure))
                    list.add(failure);
            }
        }

        return list;
    }

    private static String refactorResultTrace(ITestCase testCase) {
        final String END_TAG = ",";
        String path = testCase.getExecutionResultTrace();

        if (new File(path).exists()) {
            String oldContent = Utils.readFileContent(path);
            String newContent = oldContent.trim();
            if (newContent.endsWith(END_TAG)) {
                newContent = SpecialCharacter.OPEN_SQUARE_BRACE
                        + newContent.substring(0, newContent.length() - END_TAG.length())
                        + SpecialCharacter.CLOSE_SQUARE_BRACE;
            }
            Utils.writeContentToFile(newContent, path);
            return newContent;
        }

        return SpecialCharacter.EMPTY;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public void setActualName(String actualName) {
        this.actualName = actualName;
    }

    public void setExpectedName(String expectedName) {
        this.expectedName = expectedName;
    }

    public void setActualVal(String actualVal) {
        this.actualVal = actualVal;
    }

    public void setExpectedVal(String expectedVal) {
        this.expectedVal = expectedVal;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResultTrace failure = (ResultTrace) o;

        return Objects.equals(message, failure.message);
    }
}
