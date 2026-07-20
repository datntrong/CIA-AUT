package uet.fit.aut.execution;

public interface DriverConstant {

    String TEST_DRIVER_NAME = "{{TEST_DRIVER_NAME}}";

    String ASSERT_ENABLE = "ASSERT_ENABLE";

    String CALL_COUNTER = "AUT_fCall";

    String TEST_NAME = "AUT_test_case_name";

    String TEST_PATH_FILE_TAG = "{{INSERT_PATH_OF_TEST_PATH_HERE}}";

    String RUN_TEST = "AUT_run_test";

    String MARK = "AUT_mark";

    String EXEC_TRACE_FILE_TAG = "{{INSERT_PATH_OF_EXE_RESULT_HERE}}";

    String ASSERT_METHOD = "AUT_assert_method";

    String ASSERT_DOUBLE_METHOD = "AUT_assert_double_method";

    String ASSERT_PTR_METHOD = "AUT_assert_ptr_method";

    String ASSERT = "AUT_assert";

    String ASSERT_DOUBLE = "AUT_assert_double";

    String ASSERT_PTR = "AUT_assert_ptr";

    String ADDITIONAL_HEADERS_TAG = "/*{{INSERT_ADDITIONAL_HEADER_HERE}}*/";

    String INCLUDE_CLONE_TAG = "/*{{INSERT_CLONE_SOURCE_FILE_PATHS_HERE}}*/";

    String TEST_SCRIPTS_TAG = "/*{{INSERT_TEST_SCRIPTS_HERE}}*/";

    String COMPOUND_SET_UP = "/* Compound test case setup */";

    String COMPOUND_TEAR_DOWN = "/* Compound test case teardown */";

    String ADD_TESTS_TAG = "/*{{ADD_TESTS_STM}}*/";

    String TEST_SCRIPTS_STUB_TAG = "/*{{INSERT_TEST_SCRIPTS_FOR_STUB_HERE}}*/";

    String INCLUDE_LIB_TAG = "/*{{INSERT_INCLUDE_LIB_HERE}}*/";
}
