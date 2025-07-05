package com.nextgen.gameaggregator.core.filter;

import java.util.List;

public class SupportedVendors {

    private static final List<String> VENDOR_PATHS = List.of(
            com.nextgen.gameaggregator.vendor.pgsoft.constant.Endpoints.CLASS_NAME
    );

    private SupportedVendors() {}

    public static List<String> getPaths() {
        return VENDOR_PATHS;
    }

    public static String shouldApplyFilter(String requestURI) {
        String vendorClassName = "";
        for (String vendorPath : getPaths()) {
            String fullPathPrefix = "/api/v1/" + vendorPath + "/";
            if (requestURI.startsWith(fullPathPrefix)) {
                vendorClassName = vendorPath;
                break;
            }
        }
        return vendorClassName;
    }
}
