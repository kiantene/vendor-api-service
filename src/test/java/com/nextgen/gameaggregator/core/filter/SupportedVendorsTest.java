package com.nextgen.gameaggregator.core.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SupportedVendorsTest {

    @BeforeEach
    void setUp() throws Exception {
        SupportedVendors.setVendorPaths(List.of("vendorA", "vendorB", "testVendor"));
    }

    @Test
    void testExtractVendorClassName_matchFound() {
        String uri = "/api/v1/testVendor/game-launch";
        String result = SupportedVendors.extractVendorClassName(uri);
        assertEquals("testVendor", result);
    }

    @Test
    void testExtractVendorClassName_noMatch() {
        String uri = "/api/v1/unknownVendor/action";
        String result = SupportedVendors.extractVendorClassName(uri);
        assertEquals("", result);
    }

    @Test
    void testExtractVendorClassName_partialMatch_notValid() {
        String uri = "/api/v1/vendorBExtra/launch";
        String result = SupportedVendors.extractVendorClassName(uri);
        assertEquals("", result);
    }

    @Test
    void testExtractVendorClassName_exactPrefixMatch() {
        String uri = "/api/v1/vendorA/";
        String result = SupportedVendors.extractVendorClassName(uri);
        assertEquals("vendorA", result);
    }

    @Test
    void testUriDoesNotStartWithApi() {
        String uri = "/v1/testVendor/game-launch";
        String result = SupportedVendors.extractVendorClassName(uri);
        assertEquals("", result);
    }

    @Test
    void testUriStartsWithRootOnly() {
        String uri = "/testVendor/game-launch";
        String result = SupportedVendors.extractVendorClassName(uri);
        assertEquals("", result);
    }

    @Test
    void testCompletelyInvalidUri() {
        String uri = "invalid/uri/without/slash";
        String result = SupportedVendors.extractVendorClassName(uri);
        assertEquals("", result);
    }

    @Test
    void testEmptyUri() {
        String uri = "";
        String result = SupportedVendors.extractVendorClassName(uri);
        assertEquals("", result);
    }

    @Test
    void testNullUri() {
        String result = SupportedVendors.extractVendorClassName(null);
        assertEquals("", result);
    }
}
