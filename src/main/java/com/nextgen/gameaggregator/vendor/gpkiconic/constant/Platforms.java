package com.nextgen.gameaggregator.vendor.gpkiconic.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Platforms {
    WEB("WEB",
            "desktop"),
    H5("H5",
            "mobile");

    final String name;
    final String value;

    public static String checkPlatformCode(String platformCode) {
        Platforms platforms = Platforms.valueOf(platformCode);
        return platforms.value;
    }
}
