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


    public static String excelColumnNameFormula(int columnNumber) {
        //example output String
        // 1= A, 2 = B, 3 = c, 27 = AA, 28 = AB, 29 = AC

        StringBuilder columnLabel = new StringBuilder();

        while (columnNumber > 0) {
            int remainder = (columnNumber - 1) % 26;
            char columnChar = (char) (remainder + 'A');
            columnLabel.insert(0, columnChar);
            columnNumber = (columnNumber - 1) / 26;
        }

        return columnLabel.toString();
    }

}
