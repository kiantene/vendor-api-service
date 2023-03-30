package com.nextgen.gameaggregator.util;

import java.util.Arrays;
import java.util.stream.Collectors;

public class NameUtils {

    public static String generateUsername(Long... ids) {
        int base36 = 36;

        // adding a length value before the actual id value
        // eg.
        // 99 -> 0299
        // 999 -> 03999
        // 9999999 -> 079999999
        String joined = Arrays.stream(ids)
                .map(v -> String.format("%02d", v.toString().length()) + v) // pad left with 0
                .collect(Collectors.joining());

        // convert the string value into long
        long longVal = Long.parseLong(joined);

        // return as base36 encoded
        return Long.toString(longVal, base36);
    }
}
