package uet.fit.aut.util;

import uet.fit.aut.execution.DriverConstant;
import uet.fit.aut.parser.obj.INode;
import uet.fit.aut.search.Search;

public interface SourceConstant {

    static String getInstanceName(INode realParent) {
        String instanceVarName = Search.getScopeQualifier(realParent);
        instanceVarName = INSTANCE_VARIABLE + SpecialCharacter.UNDERSCORE + instanceVarName;
        instanceVarName = instanceVarName.replaceAll("[^\\w]", SpecialCharacter.UNDERSCORE)
                .replaceAll("_+", "_");
        return instanceVarName;
    }

    String EXPECTED_OUTPUT = "AUT_EXPECTED_OUTPUT";

    String ACTUAL_OUTPUT = "AUT_ACTUAL_OUTPUT";

    String INSTANCE_VARIABLE = "AUT_INSTANCE";

    String INSTANCE_VARIABLE_POINTER = "AUT_INSTANCE_PTR";

    String MARK_STM = DriverConstant.MARK;

    String INCREASE_FCALLS = DriverConstant.CALL_COUNTER + "++;";

    String STUB_PREFIX = "AUT_STUB_";

    String SRC_PREFIX = "AUT_SRC_";

    String EXPECTED_PREFIX = "EXPECTED_";

    String INCLUDE_PREFIX = "AUT_INCLUDE_";

    String GLOBAL_PREFIX = "AUT_GLOBAL_";

    String TEST = "TEST";

    String EXPECT_EQ = "EXPECT_EQ";

    // Assert integer value
    String ASSERT_EQ = "EXPECT_EQ";

    // Assert decimal value
    String ASSERT_NEAR = "ASSERT_NEAR";

    String PASSED_FLAG = "[  PASSED  ]";

    String FAILED_FLAG = "[  FAILED  ]";

//
//    // unix, macosx
//    // -w: disable warning
//    String COMPILE_FLAG_FOR_GOOGLETEST = " -lgtest_main  -lgtest -lpthread -lstdc++ -w";
//    String COMPILE_FLAG_FOR_CUNIT = " -lcunit -w";
//
//    // windows
//    // -w: disable warning
//    String COMPILE_FLAG_WINDOWS_FOR_GOOGLETEST = " -lgtest_main  -lgtest -w";
//    String COMPILE_FLAG_WINDOWS_FOR_CUNIT = " -lcunit -w";
//
//    static String getGTestCommand(String origin, boolean useGTest) {
//        if (Utils.isWindows()){
////            if (useGTest) {
////                // include gtest library
////                if (!origin.contains(COMPILE_FLAG_WINDOWS_FOR_GOOGLETEST))
////                    return origin + COMPILE_FLAG_WINDOWS_FOR_GOOGLETEST;
////                else
////                    return origin;
////            } else {
////                // the new compilation is modified from a version which contains google test flag
////                return origin.replace(COMPILE_FLAG_WINDOWS_FOR_GOOGLETEST, SpecialCharacter.EMPTY);
////            }
//
//            if (Environment.getInstance().getCompiler().isGPlusPlusCommand()) {
//                if (!origin.contains(COMPILE_FLAG_WINDOWS_FOR_GOOGLETEST))
//                    return origin + COMPILE_FLAG_WINDOWS_FOR_GOOGLETEST;
//                else
//                    return origin;
//            } else if (Environment.getInstance().getCompiler().isGccCommand()){
//                if (!origin.contains(COMPILE_FLAG_FOR_CUNIT))
//                    return origin + COMPILE_FLAG_FOR_CUNIT;
//                else
//                    return origin;
//            }
//        } else {
////            if (useGTest) {
//                // include gtest library
//            if (Environment.getInstance().getCompiler().isGPlusPlusCommand()) {
//                if (!origin.contains(COMPILE_FLAG_FOR_GOOGLETEST))
//                    return origin + COMPILE_FLAG_FOR_GOOGLETEST;
//                else
//                    return origin;
//            } else if (Environment.getInstance().getCompiler().isGccCommand()){
//                if (!origin.contains(COMPILE_FLAG_FOR_CUNIT))
//                    return origin + COMPILE_FLAG_FOR_CUNIT;
//                else
//                    return origin;
//            }
////            } else {
////                // the new compilation is modified from a version which contains google test flag
////                return origin.replace(COMPILE_FLAG, SpecialCharacter.EMPTY);
////            }
//        }
//        return ""; // cause error
//    }
}
