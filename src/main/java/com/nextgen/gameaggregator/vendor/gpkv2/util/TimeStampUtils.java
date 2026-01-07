package com.nextgen.gameaggregator.vendor.gpkv2.util;

public class TimeStampUtils {
    private TimeStampUtils() {}

    public static long normalizeToMillis(String timestamp) {
        if (timestamp == null) {
           return System.currentTimeMillis();
        }
        String ts = timestamp.trim();

        if (!ts.matches("\\d+")) {
            return System.currentTimeMillis();
        }
        StringBuilder sb = new StringBuilder(ts);

        while (sb.length() < 13) {
            sb.append("0");
        }

        return Long.parseLong(sb.toString());
    }
}
