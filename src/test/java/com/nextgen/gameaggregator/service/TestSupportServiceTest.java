package com.nextgen.gameaggregator.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;


import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class TestSupportServiceTest {

    private TestSupportService setupService(String field, String value) {
        //Default values
        String skipVendorUserPrefix = "load";
        String skipOperatorUserPrefix = "load";
        Boolean isTestEnvironment = true;
        String springEnv = "dev";
        String envPrefixVendorList = "2,9";

        //Change field if requested
        switch (field) {
            case "skipVendorUserPrefix" -> skipVendorUserPrefix = value;
            case "skipOperatorUserPrefix" -> skipOperatorUserPrefix = value;
            case "isTestEnvironment" -> isTestEnvironment = Boolean.parseBoolean(value);
            case "springEnv" -> springEnv = value;
            case "envPrefixVendorList" -> envPrefixVendorList = value;
        }
        return new TestSupportService(
                skipVendorUserPrefix,
                skipOperatorUserPrefix,
                isTestEnvironment,
                springEnv,
                envPrefixVendorList
        );
    }

    @Test
    void shouldSkipVendorCall_whenUsernameEqualSkipUser() {
        //Setup data
        String username = "helloworld";
        String skipUsernamePrefix = "helloworld";
        TestSupportService serviceUnderTest = setupService("skipVendorUserPrefix", skipUsernamePrefix);

        //Run test
        Boolean testResult = serviceUnderTest.shouldSkipVendorCall(username);

        //Validate result
        assertTrue(testResult);
    }

    @Test
    void shouldSkipVendorCall_whenUsernameStartsWithSkipUser() {
        //Setup data
        String username = "helloworld";
        String skipUsernamePrefix = "hello";
        TestSupportService serviceUnderTest = setupService("skipVendorUserPrefix", skipUsernamePrefix);

        //Run test
        Boolean testResult = serviceUnderTest.shouldSkipVendorCall(username);

        //Validate result
        assertTrue(testResult);
    }

    @Test
    void shouldNotSkipVendorCall_whenUsernameEndsWithSkipUser() {
        //Setup data
        String username = "helloworld";
        String skipUsernamePrefix = "world";
        TestSupportService serviceUnderTest = setupService("skipVendorUserPrefix", skipUsernamePrefix);

        //Run test
        Boolean testResult = serviceUnderTest.shouldSkipVendorCall(username);

        //Validate result
        assertFalse(testResult);
    }

    @Test
    void shouldSkipVendorCall_whenUsernameMatchesFirstInList() {
        //Setup data
        String username = "helloworld";
        String skipUsernamePrefix = "hello,thisiswrong";
        TestSupportService serviceUnderTest = setupService("skipVendorUserPrefix", skipUsernamePrefix);

        //Run test
        Boolean testResult = serviceUnderTest.shouldSkipVendorCall(username);

        //Validate result
        assertTrue(testResult);
    }

    @Test
    void shouldSkipVendorCall_whenUsernameMatchesLastInList() {
        //Setup data
        String username = "helloworld";
        String skipUsernamePrefix = "thisiswrong,hello";
        TestSupportService serviceUnderTest = setupService("skipVendorUserPrefix", skipUsernamePrefix);

        //Run test
        Boolean testResult = serviceUnderTest.shouldSkipVendorCall(username);

        //Validate result
        assertTrue(testResult);
    }

    @Test
    void shouldNotSkipVendorCall_whenUserPrefixIsEmpty() {
        //Setup data
        String username = "helloworld";
        String skipUsernamePrefix = "";
        TestSupportService serviceUnderTest = setupService("skipVendorUserPrefix", skipUsernamePrefix);

        //Run test
        Boolean testResult = serviceUnderTest.shouldSkipVendorCall(username);

        //Validate result
        assertFalse(testResult);
    }

    @Test
    void shouldNotSkipVendorCall_whenFirstInUserPrefixIsEmpty() {
        //Setup data
        String username = "helloworld";
        String skipUsernamePrefix = ",thisiswrong";
        TestSupportService serviceUnderTest = setupService("skipVendorUserPrefix", skipUsernamePrefix);

        //Run test
        Boolean testResult = serviceUnderTest.shouldSkipVendorCall(username);

        //Validate result
        assertFalse(testResult);
    }

    @Test
    void shouldAlwaysSkipVendorCall_whenEnvIsPreprod() {
        //Setup data
        String username = "helloworld"; //default userPrefix is load, so will not match
        String springEnv = "preprod";
        TestSupportService serviceUnderTest = setupService("springEnv", springEnv);

        //Run test
        Boolean testResult = serviceUnderTest.shouldSkipVendorCall(username);

        //Validate result
        assertTrue(testResult);
    }

    @Test
    void shouldNotSkipVendorCall_whenEnvIsNotPreprod() {
        //Setup data
        String username = "helloworld"; //default userPrefix is load, so will not match
        String springEnv = "qa";
        TestSupportService serviceUnderTest = setupService("springEnv", springEnv);

        //Run test
        Boolean testResult = serviceUnderTest.shouldSkipVendorCall(username);

        //Validate result
        assertFalse(testResult);
    }

    @Test
    void shouldSkipVendorCall_whenEnvIsATestEnv() {
        //Setup data
        String username = "loadtest"; //default userPrefix is load, so will match
        String isTestEnv = "true";
        TestSupportService serviceUnderTest = setupService("isTestEnvironment", isTestEnv);

        //Run test
        Boolean testResult = serviceUnderTest.shouldSkipVendorCall(username);

        //Validate result
        assertTrue(testResult);
    }

    @Test
    void shouldNotSkipVendorCall_whenEnvIsNotATestEnv() {
        //Setup data
        String username = "loadtest"; //default userPrefix is load, so will match
        String isTestEnv = "false";
        TestSupportService serviceUnderTest = setupService("isTestEnvironment", isTestEnv);

        //Run test
        Boolean testResult = serviceUnderTest.shouldSkipVendorCall(username);

        //Validate result
        assertFalse(testResult);
    }

    @Test
    void shouldSkipOperatorCall_whenUsernameEqualSkipUser() {
        //Setup data
        String username = "helloworld";
        String skipUsernamePrefix = "helloworld";
        TestSupportService serviceUnderTest = setupService("skipOperatorUserPrefix", skipUsernamePrefix);

        //Run test
        Boolean testResult = serviceUnderTest.shouldSkipOperatorCall(username);

        //Validate result
        assertTrue(testResult);
    }

    @Test
    void shouldSkipOperatorCall_whenUsernameStartsWithSkipUser() {
        //Setup data
        String username = "helloworld";
        String skipUsernamePrefix = "hello";
        TestSupportService serviceUnderTest = setupService("skipOperatorUserPrefix", skipUsernamePrefix);

        //Run test
        Boolean testResult = serviceUnderTest.shouldSkipOperatorCall(username);

        //Validate result
        assertTrue(testResult);
    }

    @Test
    void shouldNotSkipOperatorCall_whenUsernameEndsWithSkipUser() {
        //Setup data
        String username = "helloworld";
        String skipUsernamePrefix = "world";
        TestSupportService serviceUnderTest = setupService("skipOperatorUserPrefix", skipUsernamePrefix);

        //Run test
        Boolean testResult = serviceUnderTest.shouldSkipOperatorCall(username);

        //Validate result
        assertFalse(testResult);
    }

    @Test
    void shouldSkipOperatorCall_whenUsernameMatchesFirstInList() {
        //Setup data
        String username = "helloworld";
        String skipUsernamePrefix = "hello,thisiswrong";
        TestSupportService serviceUnderTest = setupService("skipOperatorUserPrefix", skipUsernamePrefix);

        //Run test
        Boolean testResult = serviceUnderTest.shouldSkipOperatorCall(username);

        //Validate result
        assertTrue(testResult);
    }

    @Test
    void shouldSkipOperatorCall_whenUsernameMatchesLastInList() {
        //Setup data
        String username = "helloworld";
        String skipUsernamePrefix = "thisiswrong,hello";
        TestSupportService serviceUnderTest = setupService("skipOperatorUserPrefix", skipUsernamePrefix);

        //Run test
        Boolean testResult = serviceUnderTest.shouldSkipOperatorCall(username);

        //Validate result
        assertTrue(testResult);
    }

    @Test
    void shouldNotSkipOperatorCall_whenUserPrefixIsEmpty() {
        //Setup data
        String username = "helloworld";
        String skipUsernamePrefix = "";
        TestSupportService serviceUnderTest = setupService("skipOperatorUserPrefix", skipUsernamePrefix);

        //Run test
        Boolean testResult = serviceUnderTest.shouldSkipOperatorCall(username);

        //Validate result
        assertFalse(testResult);
    }

    @Test
    void shouldNotSkipOperatorCall_whenFirstInUserPrefixIsEmpty() {
        //Setup data
        String username = "helloworld";
        String skipUsernamePrefix = ",thisiswrong";
        TestSupportService serviceUnderTest = setupService("skipOperatorUserPrefix", skipUsernamePrefix);

        //Run test
        Boolean testResult = serviceUnderTest.shouldSkipOperatorCall(username);

        //Validate result
        assertFalse(testResult);
    }

    @Test
    void shouldSkipOperatorCall_whenEnvIsATestEnv() {
        //Setup data
        String username = "loadtest"; //default userPrefix is load, so will match
        String isTestEnv = "true";
        TestSupportService serviceUnderTest = setupService("isTestEnvironment", isTestEnv);

        //Run test
        Boolean testResult = serviceUnderTest.shouldSkipOperatorCall(username);

        //Validate result
        assertTrue(testResult);
    }

    @Test
    void shouldNotSkipOperatorCall_whenEnvIsNotATestEnv() {
        //Setup data
        String username = "loadtest"; //default userPrefix is load, so will match
        String isTestEnv = "false";
        TestSupportService serviceUnderTest = setupService("isTestEnvironment", isTestEnv);

        //Run test
        Boolean testResult = serviceUnderTest.shouldSkipOperatorCall(username);

        //Validate result
        assertFalse(testResult);
    }

    @Test
    void shouldNotAddVendorUserPrefix_whenEnvIsNotATestEnv() {
        //Setup data
        String username = "zackqatest";
        Integer vendorId = 9; //default vendorList is [2,9], so will match
        String isTestEnv = "false";
        TestSupportService serviceUnderTest = setupService("isTestEnvironment", isTestEnv);

        //Run test
        String testResult = serviceUnderTest.appendEnvPrefixToVendorUsername(username, vendorId);

        //Validate result
        assertEquals(username, testResult);
    }

    @Test
    void shouldAddQAVendorUserPrefix_whenSpringEnvIsDev() {
        //Setup data
        String username = "zackqatest";
        Integer vendorId = 9; //default vendorList is [2,9], so will match
        String springEnv = "dev";
        TestSupportService serviceUnderTest = setupService("springEnv", springEnv);

        //Run test
        String testResult = serviceUnderTest.appendEnvPrefixToVendorUsername(username, vendorId);

        //Validate result
        var expectedUsername = "q" + username;
        assertEquals(expectedUsername, testResult);
    }

    @Test
    void shouldAddQAVendorUserPrefix_whenSpringEnvIsQA() {
        //Setup data
        String username = "zackqatest";
        Integer vendorId = 9; //default vendorList is [2,9], so will match
        String springEnv = "qa";
        TestSupportService serviceUnderTest = setupService("springEnv", springEnv);

        //Run test
        String testResult = serviceUnderTest.appendEnvPrefixToVendorUsername(username, vendorId);

        //Validate result
        var expectedUsername = "q" + username;
        assertEquals(expectedUsername, testResult);
    }

    @Test
    void shouldAddSTGVendorUserPrefix_whenSpringEnvIsSTG() {
        //Setup data
        String username = "zackqatest";
        Integer vendorId = 9; //default vendorList is [2,9], so will match
        String springEnv = "stg";
        TestSupportService serviceUnderTest = setupService("springEnv", springEnv);

        //Run test
        String testResult = serviceUnderTest.appendEnvPrefixToVendorUsername(username, vendorId);

        //Validate result
        var expectedUsername = "s" + username;
        assertEquals(expectedUsername, testResult);
    }

    @Test
    void shouldAddPreprodVendorUserPrefix_whenSpringEnvIsPreprod() {
        //Setup data
        String username = "zackqatest";
        Integer vendorId = 9; //default vendorList is [2,9], so will match
        String springEnv = "preprod";
        TestSupportService serviceUnderTest = setupService("springEnv", springEnv);

        //Run test
        String testResult = serviceUnderTest.appendEnvPrefixToVendorUsername(username, vendorId);

        //Validate result
        var expectedUsername = "p" + username;
        assertEquals(expectedUsername, testResult);
    }

    @Test
    void shouldNotAddVendorUserPrefix_whenSpringEnvIsProd() {
        //Setup data
        String username = "zackqatest";
        Integer vendorId = 9; //default vendorList is [2,9], so will match
        String springEnv = "cis";
        TestSupportService serviceUnderTest = setupService("springEnv", springEnv);

        //Run test
        String testResult = serviceUnderTest.appendEnvPrefixToVendorUsername(username, vendorId);

        //Validate result
        assertEquals(username, testResult);
    }

    @Test
    void shouldAddVendorUserPrefix_whenMatchFirstInVendorList() {
        //Setup data
        String username = "zackqatest";
        Integer vendorId = 9; //default vendorList is [2,9], so will match
        String vendorList = "9,18";
        TestSupportService serviceUnderTest = setupService("envPrefixVendorList", vendorList);

        //Run test
        String testResult = serviceUnderTest.appendEnvPrefixToVendorUsername(username, vendorId);

        //Validate result
        var expectedUsername = "q" + username;
        assertEquals(expectedUsername, testResult);
    }

    @Test
    void shouldAddVendorUserPrefix_whenMatchLastInVendorList() {
        //Setup data
        String username = "zackqatest";
        Integer vendorId = 9; //default vendorList is [2,9], so will match
        String vendorList = "2,9";
        TestSupportService serviceUnderTest = setupService("envPrefixVendorList", vendorList);

        //Run test
        String testResult = serviceUnderTest.appendEnvPrefixToVendorUsername(username, vendorId);

        //Validate result
        var expectedUsername = "q" + username;
        assertEquals(expectedUsername, testResult);
    }

    @Test
    void shouldNotAddVendorUserPrefix_whenNotMatchInVendorList() {
        //Setup data
        String username = "zackqatest";
        Integer vendorId = 9; //default vendorList is [2,9], so will match
        String vendorList = "91,95";
        TestSupportService serviceUnderTest = setupService("envPrefixVendorList", vendorList);

        //Run test
        String testResult = serviceUnderTest.appendEnvPrefixToVendorUsername(username, vendorId);

        //Validate result
        assertEquals(username, testResult);
    }

    @Test
    void shouldNotAddVendorUserPrefix_whenVendorListIsEmpty() {
        //Setup data
        String username = "zackqatest";
        Integer vendorId = 9; //default vendorList is [2,9], so will match
        String vendorList = "";
        TestSupportService serviceUnderTest = setupService("envPrefixVendorList", vendorList);

        //Run test
        String testResult = serviceUnderTest.appendEnvPrefixToVendorUsername(username, vendorId);

        //Validate result
        assertEquals(username, testResult);
    }
}

