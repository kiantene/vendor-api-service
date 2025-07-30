package com.nextgen.gameaggregator.core.filter;

import java.util.List;

public class SupportedVendors {

    private static List<String> VENDOR_PATHS = List.of(
            "aviator"
    );

    private SupportedVendors() {}

    static void setVendorPaths(List<String> paths) {
        VENDOR_PATHS = paths;
    }

    public static List<String> getPaths() {
        return VENDOR_PATHS;
    }

    public static String extractVendorClassName(String requestURI) {
        if (requestURI == null) return "";

        return getPaths().stream()
                .filter(path -> requestURI.startsWith("/api/v1/" + path + "/"))
                .findFirst()
                .orElse("");
    }
}
