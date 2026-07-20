/*
 * TEST DRIVER FOR C
 * @author: VNU-UET
 * Generate automatically by AUT
 */

// include some necessary standard libraries
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>

#define ASSERT_ENABLE

// define maximum line of test path
#define AUT_MARK_MAX 5000

// function call counter
int AUT_fCall = 0;

// test case name
char * AUT_test_case_name;

typedef void (*AUT_Test)();

void AUT_run_test(const char * name, AUT_Test test, int iterator);

////////////////////////////////////////
//  BEGIN TEST PATH SECTION           //
////////////////////////////////////////

#define AUT_TEST_PATH_FILE "{{INSERT_PATH_OF_TEST_PATH_HERE}}"

FILE* AUT_tp_file;

void AUT_append_test_path(char content[]);

int AUT_mark(char * append);

////////////////////////////////////////
//  END TEST PATH SECTION             //
////////////////////////////////////////


////////////////////////////////////////
//  BEGIN TEST RESULT SECTION         //
////////////////////////////////////////

#define AUT_EXEC_TRACE_FILE "{{INSERT_PATH_OF_EXE_RESULT_HERE}}"

FILE* AUT_rt_file;

void AUT_append_test_result(char content[]);

void AUT_assert_method
(
    char * actualName, int actualVal,
    char * expectedName, int expectedVal,
    char * method
);

void AUT_assert_double_method
(
    char * actualName, double actualVal,
    char * expectedName, double expectedVal,
    char * method
);

void AUT_assert_ptr_method
(
    char * actualName, void * actualVal,
    char * expectedName, void * expectedVal,
    char * method
);

void AUT_assert
(
    char * actualName, int actualVal,
    char * expectedName, int expectedVal
)
{
    AUT_assert_method
    (
        actualName, actualVal,
        expectedName, expectedVal,
        NULL
    );
}

int AUT_assert_double
(
    char * actualName, double actualVal,
    char * expectedName, double expectedVal
)
{
    AUT_assert_double_method
    (
        actualName, actualVal,
        expectedName, expectedVal,
        NULL
    );
}

int AUT_assert_ptr
(
    char * actualName, void * actualVal,
    char * expectedName, void * expectedVal
)
{
    AUT_assert_ptr_method
    (
        actualName, actualVal,
        expectedName, expectedVal,
        NULL
    );
}

////////////////////////////////////////
//  END TEST RESULT SECTION           //
////////////////////////////////////////


////////////////////////////////////////
//  BEGIN SET UP - TEAR DOWN SECTION  //
////////////////////////////////////////

/*
 * This function call before main test driver
 */
void AUT_set_up();

/*
 * This function call after main test driver
 */
void AUT_tear_down();

////////////////////////////////////////
//  END SET UP - TEAR DOWN SECTION    //
////////////////////////////////////////

// Some test cases need to include specific additional headers
/*{{INSERT_ADDITIONAL_HEADER_HERE}}*/

// Include autignore file
/*{{INSERT_CLONE_SOURCE_FILE_PATHS_HERE}}*/

////////////////////////////////////////
//  BEGIN TEST SCRIPTS SECTION        //
////////////////////////////////////////

/*{{INSERT_TEST_SCRIPTS_HERE}}*/

////////////////////////////////////////
//  END TEST SCRIPTS SECTION          //
////////////////////////////////////////

/* 
 * The main() function for setting up and running the tests.
 */
int main()
{
    AUT_set_up();
    
    /* Compound test case setup */

    /* add & run the tests */
/*{{ADD_TESTS_STM}}*/

    /* Compound test case teardown */

    AUT_tear_down();
    
    return 0;
}

////////////////////////////////////////
//  BEGIN DEFINITIONS SECTION         //
////////////////////////////////////////

void AUT_append_test_path(char content[])
{
    static int aut_mark_iterator = 0;
    
    AUT_tp_file = fopen(AUT_TEST_PATH_FILE, "a");
    fputs(content, AUT_tp_file);
    aut_mark_iterator++;

    // if the test path is too long, we need to terminate the process
    if (aut_mark_iterator >= AUT_MARK_MAX) {
        fputs("\nThe test path is too long. Terminate the program automatically!", AUT_tp_file);
        fclose(AUT_tp_file);
        exit(0);
    }

    fclose(AUT_tp_file);
}

void AUT_append_test_result(char content[])
{
    AUT_rt_file = fopen(AUT_EXEC_TRACE_FILE, "a");
    fputs(content, AUT_rt_file);
    fclose(AUT_rt_file);
}

#define AUT_MAX_LINE_LENGTH 100000

int AUT_mark(char * append)
{
    char build[AUT_MAX_LINE_LENGTH] = "";
    strcat(build, append);
    strcat(build, "\n");
    AUT_append_test_path(build);
    return 1;
}

#define AUT_BUFFER_SIZE 1024

void AUT_assert_method
(
    char * actualName, int actualVal,
    char * expectedName, int expectedVal,
    char * userCode
)
{
    char buf[AUT_MAX_LINE_LENGTH] = "{\n";

    strcat(buf, "\"tag\": \"aut function calls: ");
    char temp0[AUT_BUFFER_SIZE];
    sprintf(temp0, "%d\",", AUT_fCall);
    strcat(buf, temp0);
    strcat(buf, "\n");

    if (userCode != NULL)
    {
        strcat(buf, "\"userCode\": \"");
        strcat(buf, userCode);
        strcat(buf, "\",\n");
    }

    strcat(buf, "\"actualName\": \"");
    strcat(buf, actualName);
    strcat(buf, "\",\n");
    char temp1[AUT_BUFFER_SIZE];
    sprintf(temp1, "\"actualVal\": \"%d\",", actualVal);
    strcat(buf, temp1);
    strcat(buf, "\n");

    strcat(buf, "\"expectedName\": \"");
    strcat(buf, expectedName);
    strcat(buf, "\",\n");
    char temp2[AUT_BUFFER_SIZE];
    sprintf(temp2, "\"expectedVal\": \"%d\"", expectedVal);
    strcat(buf, temp2);
    strcat(buf, "\n},\n");

    AUT_append_test_result(buf);
}

void AUT_assert_double_method
(
    char * actualName, double actualVal,
    char * expectedName, double expectedVal,
    char * userCode
)
{
    char buf[AUT_MAX_LINE_LENGTH] = "{\n";

    strcat(buf, "\"tag\": \"aut function calls: ");
    char temp0[AUT_BUFFER_SIZE];
    sprintf(temp0, "%d\",", AUT_fCall);
    strcat(buf, temp0);
    strcat(buf, "\n");

    if (userCode != NULL) {
        strcat(buf, "\"userCode\": \"");
        strcat(buf, userCode);
        strcat(buf, "\",\n");
    }

    strcat(buf, "\"actualName\": \"");
    strcat(buf, actualName);
    strcat(buf, "\",\n");

    char temp1[AUT_BUFFER_SIZE];
    sprintf(temp1, "\"actualVal\": \"%lf\",", actualVal);
    strcat(buf, temp1);
    strcat(buf, "\n");

    strcat(buf, "\"expectedName\": \"");
    strcat(buf, expectedName);
    strcat(buf, "\",\n");

    char temp2[AUT_BUFFER_SIZE];
    sprintf(temp2, "\"expectedVal\": \"%lf\"", expectedVal);
    strcat(buf, temp2);
    strcat(buf, "\n},\n");

    AUT_append_test_result(buf);
}

void AUT_assert_ptr_method
(
    char * actualName, void * actualVal,
    char * expectedName, void * expectedVal,
    char * userCode
)
{
    char buf[AUT_MAX_LINE_LENGTH] = "{\n";

    strcat(buf, "\"tag\": \"aut function calls: ");
    char temp0[AUT_BUFFER_SIZE];
    sprintf(temp0, "%d\",", AUT_fCall);
    strcat(buf, temp0);
    strcat(buf, "\n");

    if (userCode != NULL) {
        strcat(buf, "\"userCode\": \"");
        strcat(buf, userCode);
        strcat(buf, "\",\n");
    }

    strcat(buf, "\"actualName\": \"");
    strcat(buf, actualName);
    strcat(buf, "\",\n");

    char temp1[AUT_BUFFER_SIZE];
    sprintf(temp1, "\"actualVal\": \"%x\",", actualVal);
    strcat(buf, temp1);
    strcat(buf, "\n");

    strcat(buf, "\"expectedName\": \"");
    strcat(buf, expectedName);
    strcat(buf, "\",\n");

    char temp2[AUT_BUFFER_SIZE];
    sprintf(temp2, "\"expectedVal\": \"%x\"", expectedVal);
    strcat(buf, temp2);
    strcat(buf, "\n},\n");

    AUT_append_test_result(buf);
}

void AUT_run_test(const char * name, AUT_Test test, int iterator)
{
    char begin[AUT_BUFFER_SIZE];
    sprintf(begin, "BEGIN OF %s", name);
    AUT_mark(begin);

    int i;
    for (i = 0; i < iterator; i++) {
        test();
    }

    char end[AUT_BUFFER_SIZE];
    sprintf(end, "END OF %s", name);
    AUT_mark(end);
}

void AUT_set_up()
{
    /*{{INSERT_SET_UP_HERE}}*/
}

void AUT_tear_down()
{
    /*{{INSERT_TEAR_DOWN_HERE}}*/
}

////////////////////////////////////////
//  END DEFINITIONS SECTION           //
////////////////////////////////////////