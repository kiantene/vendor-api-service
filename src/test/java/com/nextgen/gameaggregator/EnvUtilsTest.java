package com.nextgen.gameaggregator;

import com.nextgen.gameaggregator.util.EnvUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EnvUtilsTest {

    @Test
    void testGetVendorListEmptyList() {
        //Prep data
        String testList = "";
        List<Integer> expectedResult = List.of();

        //Send test
        List<Integer> actualResult = EnvUtils.getVendorListFromEnv(testList);

        //Verify test
        String testMsg = "Expected: " + expectedResult + " - Actual: " + actualResult;
        assertEquals(expectedResult, actualResult, testMsg);
    }

    @Test
    void testGetVendorListSingleElement() {
        //Prep data
        String testList = "2";
        List<Integer> expectedResult = List.of(2);

        //Send test
        List<Integer> actualResult = EnvUtils.getVendorListFromEnv(testList);

        //Verify test
        String testMsg = "Expected: " + expectedResult + " - Actual: " + actualResult;
        assertEquals(expectedResult, actualResult, testMsg);
    }

    @Test
    void testGetVendorListMultipleElements() {
        //Prep data
        String testList = "2,13,18";
        List<Integer> expectedResult = List.of(2, 13, 18);

        //Send test
        List<Integer> actualResult = EnvUtils.getVendorListFromEnv(testList);

        //Verify test
        String testMsg = "Expected: " + expectedResult + " - Actual: " + actualResult;
        assertEquals(expectedResult, actualResult, testMsg);
    }

    @Test
    void testGetVendorListInvalidElement() {
        //Prep data
        String testList = "2,13,a";
        List<Integer> expectedResult = List.of();

        //Send test
        List<Integer> actualResult = EnvUtils.getVendorListFromEnv(testList);

        //Verify test
        String testMsg = "Expected: " + expectedResult + " - Actual: " + actualResult;
        assertEquals(expectedResult, actualResult, testMsg);
    }

    @Test
    void testGetVendorHashSetEmptyList() {
        //Prep data
        String testList = "";
        HashSet<Integer> expectedResult = new HashSet<>();

        //Send test
        HashSet<Integer> actualResult = EnvUtils.getVendorHashSetFromEnv(testList);

        //Verify test
        String testMsg = "Expected: " + expectedResult + " - Actual: " + actualResult;
        assertEquals(expectedResult, actualResult, testMsg);
    }

    @Test
    void testGetVendorHashSetSingleElement() {
        //Prep data
        String testList = "3";
        HashSet<Integer> expectedResult = new HashSet<>(List.of(3));

        //Send test
        HashSet<Integer> actualResult = EnvUtils.getVendorHashSetFromEnv(testList);

        //Verify test
        String testMsg = "Expected: " + expectedResult + " - Actual: " + actualResult;
        assertEquals(expectedResult, actualResult, testMsg);
    }

    @Test
    void testGetVendorHashSetMultipleElements() {
        //Prep data
        String testList = "3,13,17";
        HashSet<Integer> expectedResult = new HashSet<>(List.of(3, 13, 17));

        //Send test
        HashSet<Integer> actualResult = EnvUtils.getVendorHashSetFromEnv(testList);

        //Verify test
        String testMsg = "Expected: " + expectedResult + " - Actual: " + actualResult;
        assertEquals(expectedResult, actualResult, testMsg);
    }

    @Test
    void testGetVendorHashSetInvalidElement() {
        //Prep data
        String testList = "3,13,a";
        HashSet<Integer> expectedResult = new HashSet<>(List.of(3, 13));

        //Send test
        HashSet<Integer> actualResult = EnvUtils.getVendorHashSetFromEnv(testList);

        //Verify test
        String testMsg = "Expected: " + expectedResult + " - Actual: " + actualResult;
        assertEquals(expectedResult, actualResult, testMsg);
    }
}
