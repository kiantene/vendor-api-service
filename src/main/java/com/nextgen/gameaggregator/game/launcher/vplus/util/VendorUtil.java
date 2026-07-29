package com.nextgen.gameaggregator.game.launcher.vplus.util;

import java.util.Map;
import java.util.TreeMap;

public class VendorUtil {
    private static final String APP_ID = "appId";
    private static final String TIME_STAMP = "timestamp";
    private static final String USERNAME = "username";

    private VendorUtil() {}

    public static String generateSign(Map<String, String> params) {

        TreeMap<String, String> sortedParams = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();

        sortedParams.forEach((key, value) ->
                sb.append(key).append("=").append(value).append("&")
        );
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public static Map<String, String> sortedParams(Map<String, String> params) {
        Map<String, String> sortedParams = new TreeMap<>();
        sortedParams.put(APP_ID, params.get(APP_ID));
        sortedParams.put(TIME_STAMP, params.get(TIME_STAMP));
        sortedParams.put(USERNAME, params.get(USERNAME));
        return sortedParams;
    }
}
