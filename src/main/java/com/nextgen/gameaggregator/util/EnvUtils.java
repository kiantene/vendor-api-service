package com.nextgen.gameaggregator.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class EnvUtils {
    public static List<Integer> getVendorListFromEnv(String value) {
        // Convert the string to a list of integers
        if (value == null || value.isBlank()) {
            return new ArrayList<>();  // Default empty list
        } else {
            try {
                return Arrays.stream(value.split(","))
                        .map(String::trim) // Trim spaces from each element
                        .map(Integer::parseInt)
                        .toList();
            } catch (Exception e) {
                // If parsing fails, return an empty list
                return new ArrayList<>();
            }
        }
    }

    //HashSet is more performant when handling lookup in a large list.
    //But not sure how much better it will be for list of only ~50 elements
    public static HashSet<Integer> getVendorHashSetFromEnv(String value) {
        HashSet<Integer> set = new HashSet<>();

        try {
            String[] list = value.split(",");

            for (String inputValue : list) {
                int inputNum = Integer.parseInt(inputValue.trim());
                set.add(inputNum);
            }
        } catch (Exception e) {
            // If empty list, will return blank set
            // If unable to parse, will return existing hashset so far
            return set;
        }
        return set;
    }

}
