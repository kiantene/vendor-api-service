package com.nextgen.gameaggregator.util;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@UtilityClass
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
}
