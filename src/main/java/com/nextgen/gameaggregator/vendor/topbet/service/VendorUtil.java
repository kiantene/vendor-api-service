package com.nextgen.gameaggregator.vendor.topbet.service;

import org.apache.commons.codec.digest.DigestUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VendorUtil {
    private VendorUtil() {
    }

    public static String getSignature(Map<String, Object> args, String secretKey) {
        List<String> keys = new ArrayList<>(args.keySet());
        keys = keys.stream()
                .filter(k -> !"sign".equals(k))
                .sorted() //sorted by ASCII code in ascending order
                .toList();

        StringBuilder stringToSign = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            Object value = args.get(key);
            stringToSign.append(key).append("=").append(value == null ? "" : value.toString());
            if (i < keys.size() - 1) {
                stringToSign.append("&");
            }
        }
        //Append secret key
        stringToSign.append("&apikey=").append(secretKey);

        // Return uppercase MD5
        return DigestUtils.md5Hex(stringToSign.toString()).toUpperCase();
    }

    public static BigDecimal formatBalance(BigDecimal balance) {
        if (balance == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.DOWN);
        }
        return balance.setScale(2, RoundingMode.DOWN);
    }

    public static long formatTimestamp(long tenDigitTimestamp) {
        return tenDigitTimestamp * 1000L;
    }
}
