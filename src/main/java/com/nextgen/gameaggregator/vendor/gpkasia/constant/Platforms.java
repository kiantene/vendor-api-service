package com.nextgen.gameaggregator.vendor.gpkasia.constant;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Platforms {

    WEB("WEB","desktop"),
    H5("H5","mobile");

    String name;
    String value;

    public static String checkPlatformCode(String plstformCode){
        Platforms platforms = Platforms.valueOf(plstformCode);
        return platforms.value;
    }
}
