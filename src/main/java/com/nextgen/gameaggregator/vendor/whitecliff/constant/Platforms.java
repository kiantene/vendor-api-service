package com.nextgen.gameaggregator.vendor.whitecliff.constant;

public class Platforms {

    public static final String WEB = "desktop";
    public static final String H5 = "mobile";

    private Platforms() {
    }

    public static Platforms createPlatforms() {
        return new Platforms();
    }
}