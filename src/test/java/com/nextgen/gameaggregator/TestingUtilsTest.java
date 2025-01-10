package com.nextgen.gameaggregator;

import com.nextgen.gameaggregator.util.TestingUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestingUtilsTest {

    @Test
    void testAddEnvPrefixforDev() {
        String env = "dev";
        String username = "random";
        TestingUtils classInTest = new TestingUtils();
        String actualResult = classInTest.addEnvToUsername(username, env);
        String expectedResult = "qrandom";
        String testMsg = "Expected: " + expectedResult + " - Actual: " + actualResult;

        assertEquals(expectedResult, actualResult, testMsg);
    }

    @Test
    void testAddEnvPrefixforQA() {
        String env = "qa";
        String username = "random";
        TestingUtils classInTest = new TestingUtils();
        String actualResult = classInTest.addEnvToUsername(username, env);
        String expectedResult = "qrandom";
        String testMsg = "Expected: " + expectedResult + " - Actual: " + actualResult;

        assertEquals(expectedResult, actualResult, testMsg);
    }

    @Test
    void testAddEnvPrefixforSTG() {
        String env = "stg";
        String username = "random";
        TestingUtils classInTest = new TestingUtils();
        String actualResult = classInTest.addEnvToUsername(username, env);
        String expectedResult = "srandom";
        String testMsg = "Expected: " + expectedResult + " - Actual: " + actualResult;

        assertEquals(expectedResult, actualResult, testMsg);
    }

    @Test
    void testAddEnvPrefixforPreprod() {
        String env = "preprod";
        String username = "random";
        TestingUtils classInTest = new TestingUtils();
        String actualResult = classInTest.addEnvToUsername(username, env);
        String expectedResult = "prandom";
        String testMsg = "Expected: " + expectedResult + " - Actual: " + actualResult;

        assertEquals(expectedResult, actualResult, testMsg);
    }

    @Test
    void testAddEnvPrefixforProd() {
        String env = "latam";
        String username = "random";
        TestingUtils classInTest = new TestingUtils();
        String actualResult = classInTest.addEnvToUsername(username, env);
        String expectedResult = "random";
        String testMsg = "Expected: " + expectedResult + " - Actual: " + actualResult;

        assertEquals(expectedResult, actualResult, testMsg);
    }
}
