/*
 * TEST DRIVER FOR C++
 * @author: VNU-UET
 * Generate automatically by AUT
 */

// include some necessary standard libraries
#include <cstdio>
#include <string>
#include <fstream>
#include <cstring>
#include <QtTest/QtTest>
#include <QTest>
/*{{INSERT_INCLUDE_LIB_HERE}}*/


class TestDriver: public QObject
{
    Q_OBJECT

private slots:
    void mainTest();
};

#define ASSERT_ENABLE

// define maximum line of test path
#define AUT_MARK_MAX 5000

// function call counter
int AUT_fCall = 0;

// test case name
char* AUT_test_case_name;

typedef void (*AUT_Test)();

void AUT_run_test(std::string name, AUT_Test test, int iterator);

////////////////////////////////////////
//  BEGIN TEST PATH SECTION           //
////////////////////////////////////////

#define AUT_TEST_PATH_FILE "{{INSERT_PATH_OF_TEST_PATH_HERE}}"

void AUT_append_test_path(std::string content);

int AUT_mark(std::string append);

int AUT_mark(char * append)
{
    std::string str(append);
    return AUT_mark(str);
}

////////////////////////////////////////
//  END TEST PATH SECTION             //
////////////////////////////////////////


////////////////////////////////////////
//  BEGIN TEST RESULT SECTION         //
////////////////////////////////////////

#define AUT_EXEC_TRACE_FILE "{{INSERT_PATH_OF_EXE_RESULT_HERE}}"

void AUT_append_test_result(std::string content);

void AUT_assert_method
(
    std::string actualName, int actualVal,
    std::string expectedName, int expectedVal,
    std::string method
);

void AUT_assert_double_method
(
    std::string actualName, double actualVal,
    std::string expectedName, double expectedVal,
    std::string method
);

void AUT_assert_ptr_method
(
    std::string actualName, void* actualVal,
    std::string expectedName, void* expectedVal,
    std::string method
);

#define NULL_STRING ""

void AUT_assert
(
    std::string actualName, int actualVal,
    std::string expectedName, int expectedVal
)
{
    AUT_assert_method
    (
        actualName, actualVal,
        expectedName, expectedVal,
        NULL_STRING
    );
}

int AUT_assert_double
(
    std::string actualName, double actualVal,
    std::string expectedName, double expectedVal
)
{
    AUT_assert_double_method
    (
        actualName, actualVal,
        expectedName, expectedVal,
        NULL_STRING
    );
}

int AUT_assert_ptr
(
    std::string actualName, void* actualVal,
    std::string expectedName, void* expectedVal
)
{
    AUT_assert_ptr_method
    (
        actualName, actualVal,
        expectedName, expectedVal,
        NULL_STRING
    );
}

void AUT_assert
(
    char* actualName, int actualVal,
    char* expectedName, int expectedVal
)
{
    std::string strAct(actualName);
    std::string strExp(expectedName);
    AUT_assert(strAct, actualVal, strExp, expectedVal);
}

int AUT_assert_double
(
    char* actualName, double actualVal,
    char* expectedName, double expectedVal
)
{
    std::string strAct(actualName);
    std::string strExp(expectedName);
    AUT_assert_double(strAct, actualVal, strExp, expectedVal);
}

int AUT_assert_ptr
(
    char* actualName, void* actualVal,
    char* expectedName, void* expectedVal
)
{
    std::string strAct(actualName);
    std::string strExp(expectedName);
    AUT_assert_ptr(strAct, actualVal, strExp, expectedVal);
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
//     BEGIN TEST SCRIPTS STUB        //
////////////////////////////////////////

/*{{INSERT_TEST_SCRIPTS_FOR_STUB_HERE}}*/

////////////////////////////////////////
//     END TEST SCRIPTS STUB          //
////////////////////////////////////////


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
 QTEST_MAIN(TestDriver)

////////////////////////////////////////
//  BEGIN DEFINITIONS SECTION         //
////////////////////////////////////////

void AUT_append_test_path(std::string content)
{
    static int aut_mark_iterator = 0;

    std::ofstream outfile;
    outfile.open(AUT_TEST_PATH_FILE, std::ios_base::app);
    outfile << content;
    aut_mark_iterator++;

    // if the test path is too long, we need to terminate the process
    if (aut_mark_iterator >= AUT_MARK_MAX) {
        outfile << "\nThe test path is too long. Terminate the program automatically!";
        outfile.close();
        exit(0);
    }

    outfile.close();
}

void AUT_append_test_result(std::string content)
{
    std::ofstream outfile;
    outfile.open(AUT_EXEC_TRACE_FILE, std::ios_base::app);
    outfile << content;
    outfile.close();
}

int AUT_mark(std::string append)
{
    AUT_append_test_path(append + "\n");
    return 1;
}

#define AUT_BUFFER_SIZE 1024

void AUT_assert_method
(
    std::string actualName, int actualVal,
    std::string expectedName, int expectedVal,
    std::string userCode
)
{
    std::string buf = "{\n";

    buf.append("\"tag\": \"aut function calls: ");
    char temp0[AUT_BUFFER_SIZE];
    sprintf(temp0, "%d\",", AUT_fCall);
    buf.append(temp0);
    buf.append("\n");

    if (!userCode.empty())
    {
        buf.append("\"userCode\": \"");
        buf.append(userCode);
        buf.append("\",\n");
    }

    buf.append("\"actualName\": \"");
    buf.append(actualName);
    buf.append("\",\n");
    char temp1[AUT_BUFFER_SIZE];
    sprintf(temp1, "\"actualVal\": \"%d\",", actualVal);
    buf.append(temp1);
    buf.append("\n");

    buf.append("\"expectedName\": \"");
    buf.append(expectedName);
    buf.append("\",\n");
    char temp2[AUT_BUFFER_SIZE];
    sprintf(temp2, "\"expectedVal\": \"%d\"", expectedVal);
    buf.append(temp2);
    buf.append("\n},\n");

    AUT_append_test_result(buf);
}

void AUT_assert_double_method
(
    std::string actualName, double actualVal,
    std::string expectedName, double expectedVal,
    std::string userCode
)
{
    std::string buf = "{\n";

    buf.append("\"tag\": \"aut function calls: ");
    char temp0[AUT_BUFFER_SIZE];
    sprintf(temp0, "%d\",", AUT_fCall);
    buf.append(temp0);
    buf.append("\n");

    if (!userCode.empty())
    {
        buf.append("\"userCode\": \"");
        buf.append(userCode);
        buf.append("\",\n");
    }

    buf.append("\"actualName\": \"");
    buf.append(actualName);
    buf.append("\",\n");

    char temp1[AUT_BUFFER_SIZE];
    sprintf(temp1, "\"actualVal\": \"%lf\",", actualVal);
    buf.append(temp1);
    buf.append("\n");

    buf.append("\"expectedName\": \"");
    buf.append(expectedName);
    buf.append("\",\n");

    char temp2[AUT_BUFFER_SIZE];
    sprintf(temp2, "\"expectedVal\": \"%lf\"", expectedVal);
    buf.append(temp2);
    buf.append("\n},\n");

    AUT_append_test_result(buf);
}

void AUT_assert_ptr_method
(
    std::string actualName, void * actualVal,
    std::string expectedName, void * expectedVal,
    std::string userCode
)
{
    std::string buf = "{\n";

    buf.append("\"tag\": \"aut function calls: ");
    char temp0[AUT_BUFFER_SIZE];
    sprintf(temp0, "%d\",", AUT_fCall);
    buf.append(temp0);
    buf.append("\n");

    if (!userCode.empty())
    {
        buf.append("\"userCode\": \"");
        buf.append(userCode);
        buf.append("\",\n");
    }

    buf.append("\"actualName\": \"");
    buf.append(actualName);
    buf.append("\",\n");

    char temp1[AUT_BUFFER_SIZE];
    sprintf(temp1, "\"actualVal\": \"%x\",", actualVal);
    buf.append(temp1);
    buf.append("\n");

    buf.append("\"expectedName\": \"");
    buf.append(expectedName);
    buf.append("\",\n");

    char temp2[AUT_BUFFER_SIZE];
    sprintf(temp2, "\"expectedVal\": \"%x\"", expectedVal);
    buf.append(temp2);
    buf.append("\n},\n");

    AUT_append_test_result(buf);
}

void AUT_run_test(std::string name, AUT_Test test, int iterator)
{
    std::string begin = "BEGIN OF " + name;
    AUT_mark(begin);

    int i;
    for (i = 0; i < iterator; i++) {
        test();
    }

    std::string end = "END OF " + name;
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
#include "{{TEST_DRIVER_NAME}}"
