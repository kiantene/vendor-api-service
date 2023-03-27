package com.nextgen.gameaggregator.vendor.spinix.service;

import org.apache.commons.codec.digest.DigestUtils;
import java.util.*;
import java.util.stream.Collectors;

public class VendorService {

    public String getSignature(Map<String, Object> args, String signatureKey) {
        String value = signatureKey;
        Map<String, Object> keyObject = _getKeyValueFromObject(args, "");
        List<String> keys = new ArrayList<>(keyObject.keySet());
        keys = keys.stream().filter(item -> !item.equals("signature")).collect(Collectors.toList());
        Collections.sort(keys);
        for (String key : keys) {
            value += "&" + key + "=" + keyObject.get(key);
        }
        String token = DigestUtils.md5Hex(value);
        return token;
    }

    public Map<String, Object> _getKeyValueFromObject(Map<String, Object> args, String prefixKey) {
        Map<String, Object> result = new HashMap<>();
        for (String key : args.keySet()) {
            if (args.get(key) == null) {
                continue;
            }

            if (!(args.get(key) instanceof Map) && !(args.get(key) instanceof ArrayList)) {
                String resultKey = "";
                if (!prefixKey.isEmpty()) {
                    resultKey = prefixKey + ".";
                }
                resultKey += key;
                result.put(resultKey, args.get(key));
                continue;
            }

            if(args.get(key) instanceof Map) {
                if (!((Map) args.get(key)).isEmpty()) {
                    String nestedPrefixKey = key;
                    if (!prefixKey.isEmpty()) {
                        nestedPrefixKey = prefixKey + "." + key;
                    }
                    Map<String, Object> nestedResult = _getKeyValueFromObject((Map<String, Object>) args.get(key), nestedPrefixKey);
                    result.putAll(nestedResult);
                }
            }

            if(args.get(key) instanceof ArrayList) {
                ArrayList data = (ArrayList) args.get(key);
                Map<String, Object> map = new HashMap<>();
                if (!data.isEmpty()) {

                    for(int i = 0; i < data.size(); i++) {
                        String nestedPrefixKey = key + "." + i;
                        Map<String, Object> nestedResult = _getKeyValueFromObject((Map<String, Object>) data.get(i), nestedPrefixKey);
                        result.putAll(nestedResult);
                    }
                }
            }

        }
        return result;
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
