package com.nextgen.gameaggregator.util;

import org.apache.commons.lang3.StringUtils;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class NameUtils {
    public static String getUsername(Long userId) {
        return Base64.getEncoder().encodeToString(BigInteger.valueOf(userId).toByteArray());
    }

    public static String getUsername(String userId) {
        return Base64.getEncoder().encodeToString(userId.getBytes());
    }

    public static String generateUsername(String separator, Long... ids) {
        // Convert ids into array stream and apply base58 encoding function to each id in the array
        List<String> elementList = Arrays.stream(ids)
                .map(v -> Base58.encode((BigInteger.valueOf(v).toByteArray())))
                .toList();

        // The reason for using base58 is that some vendors may not accept special characters in their username format
        // and we can use one of the 4 characters (0OIl) that were excluded in base58 as separator to join the ids

        // After encoding all ids, join the ids using the provided separator
        return StringUtils.join(elementList, separator);
    }
}
