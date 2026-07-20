package uet.fit.aut.execution.testdriver;

import uet.fit.aut.execution.DriverConstant;

public interface IDriverGeneration {

    String CLONED_SOURCE_FILE_PATH_TAG = DriverConstant.INCLUDE_CLONE_TAG;

    String C_TEST_DRIVER_PATH = "/test-driver-templates/testdriver.c";
    String CPP_TEST_DRIVER_PATH = "/test-driver-templates/testdriver.cpp";

    void generate() throws Exception;

    String getTestDriver();
}
