package com.nextgen.gameaggregator.vendor.pragmaticplay.service;

import com.nextgen.gameaggregator.exception.InvalidSignatureException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class VendorService extends BaseVendorService {
    public static String getTransactionId(String transactionId) {
        return transactionId.replace("-", "");
    }

    public static String generateHash(MultiValueMap<String, String> params, String secret) {
        String payload = params.keySet().stream().sorted()
                .map(key -> key + "=" + params.get(key).get(0))
                .collect(Collectors.joining("&"));

        return generateHash(payload, secret);
    }

    public static String generateHash(Map<String, String> params, String secret) {
        String payload = params.keySet().stream().sorted()
                .map(key -> key + "=" + params.get(key))
                .collect(Collectors.joining("&"));

        return generateHash(payload, secret);
    }

    public static String generateHash(String payload, String secret) {
        payload += secret;
        return DigestUtils.md5Hex(payload);
    }

    public static Map<String, String> convertQueryStringToMap(String queryString) {
        Map<String, String> queryParameterMap = new HashMap<>();
        String[] fields = queryString.split("&");

        for (String field : fields) {
            String[] kv = field.split("=");
            if (kv.length == 2) queryParameterMap.put(kv[0], kv[1]);
        }

        return queryParameterMap;
    }

    public static void verifyHash(String requestBody, String secretKey) throws InvalidSignatureException {

        try {
            requestBody = URLDecoder.decode(requestBody, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            //do nothing, and use the original requestBody.
            log.error("URLDecoder.decode failed with " + e + " | requestBody = " + requestBody);
        }

        Map<String, String> map = convertQueryStringToMap(requestBody);
        String hash = map.get("hash");
        map.remove("hash");

        String generatedHash = generateHash(map, secretKey);
        if (!hash.equals(generatedHash)) {
            String msg = "Expected hash: " + generatedHash + ", but received: " + hash;
            log.error("Request body: " + requestBody);
            log.error(msg);
            throw new InvalidSignatureException(msg);
        }
    }
}
