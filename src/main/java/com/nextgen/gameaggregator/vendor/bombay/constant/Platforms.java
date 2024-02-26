package com.nextgen.gameaggregator.vendor.bombay.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Platforms {

    WEB("WEB","GPL_DESKTOP"),
    H5("H5","GPL_MOBILE");

    String name;
    String value;

    public static String checkPlatformCode(String plstformCode){
        Platforms platforms = Platforms.valueOf(plstformCode);
        return platforms.value;
    }
}
