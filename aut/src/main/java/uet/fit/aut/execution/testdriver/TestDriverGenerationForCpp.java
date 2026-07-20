package uet.fit.aut.execution.testdriver;

import uet.fit.aut.execution.DriverConstant;
import uet.fit.aut.util.Utils;

/**
 * Old name: TestdriverGenerationforCpp
 *
 * Generate test driver for function put in an .cpp file in executing test data entering by users
 * <p>
 * comparing EO and RO
 *
 * @author ducanhnguyen
 */
public class TestDriverGenerationForCpp extends AssertableTestDriverGeneration {

    @Override
    public String getTestDriverTemplate() {
        return Utils.readResourceContent(CPP_TEST_DRIVER_PATH);
    }

    protected String wrapScriptInTryCatch(String script) {
        return String.format(
                "try {\n" +
                        "%s\n" +
                        "} catch (std::exception& error) {\n" +
                        "std::string content = \"Executing test case throw an exception: \";\n" +
                        "content.append(error.what());\n" +
                        DriverConstant.MARK + "(content);\n" +
                        "printf(content.c_str());\n" +
                        "}\n" +
                        "exit(0);", script);
    }
}
