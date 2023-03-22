package com.nextgen.gameaggregator.vendor.spinix.service;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class VendorService {

    public String getSignature(Map<String, Object> args, String signatureKey) {
        String str = signatureKey;
        Map<String, Object> keyObject = _getKeyValueFromObject(args, "");
        keyObject.remove("signature");
        List<String> keys = new ArrayList<>(keyObject.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            Object value = keyObject.get(key);
            if (value instanceof Boolean) value = ((boolean) value) ? "true" : "false";
            str += "&" + key + "=" + value;
        }

        String result = this.md5(str);

        return result;
    }

    public Map<String, Object> _getKeyValueFromObject(Map<String, Object> args, String prefixKey) {
        Map<String, Object> result = new HashMap<>();
        for (String key : args.keySet()) {
            Object value = args.get(key);

            if (value == null) {
                continue;
            }

            if (!(value instanceof Map)) {
                String resultKey = "";
                if (!prefixKey.isEmpty()) {
                    resultKey = prefixKey + ".";
                }
                resultKey += key;
                result.put(resultKey, value);
                continue;
            }

            if (!args.isEmpty()) {
                String nestedPrefixKey = key;
                if (!prefixKey.isEmpty()) {
                    nestedPrefixKey = prefixKey + "." + key;
                }
                Map<String, Object> nestedResult = _getKeyValueFromObject((Map<String, Object>) value, nestedPrefixKey);
                result.putAll(nestedResult);
            }
        }
        return result;
    }

    public String md5(String input) {
        try {
            String result = input;
            if(input != null) {
                MessageDigest md = MessageDigest.getInstance("MD5"); //or "SHA-1"
                md.update(input.getBytes());
                BigInteger hash = new BigInteger(1, md.digest());
                result = hash.toString(16);
                while(result.length() < 32) { //40 for SHA-1
                    result = "0" + result;
                }
            }
            return result;
        }catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static Boolean isSameSignature(String token, Map<String, Object> body, String signatureKey) {
        VendorService vendorService = new VendorService();
        String sign = vendorService.getSignature(body, signatureKey);
        if(token.equals(sign)) {
            return true;
        }
        return false;
    }
}
