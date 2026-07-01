package com.nextgen.gameaggregator.vendor.mtlive.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Headers {
    public static final String API_CI = "APICI";   // 請求的連線端ID
    public static final String API_SI = "APISI";   // 請求的簽章
    public static final String API_TS = "APITS";   // 請求的時間戳記 (unix timestamp)
}
