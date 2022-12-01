package com.nextgen.gameaggregator.vendor.util;

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
        List<String> elementList = Arrays.stream(ids)
                .map(v -> BigInteger.valueOf(v).toByteArray())
                .map(Base58::encode)
                .toList();

        return StringUtils.join(elementList, separator);
    }
}
