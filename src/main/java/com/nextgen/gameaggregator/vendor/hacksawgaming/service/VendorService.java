package com.nextgen.gameaggregator.vendor.hacksawgaming.service;

import org.apache.commons.codec.digest.DigestUtils;

public class VendorService {

    public static String getSign(String data) {
        String token = DigestUtils.md5Hex(data);
        return token.toUpperCase();
    }

    public static boolean isSameSignature(String sign, String toVerifySign) {
        Boolean result = false;
        if(sign.equals(toVerifySign)) {
            result = true;
        }
        return result;
    }

    public static String removeDashes(String str) {
        return str.replaceAll("-", "");
    }

    public static String revertToUUID(String uuidString) {
        StringBuilder sb = new StringBuilder(uuidString);
        sb.insert(8, "-");
        sb.insert(13, "-");
        sb.insert(18, "-");
        sb.insert(23, "-");

        return sb.toString();
    }
}
