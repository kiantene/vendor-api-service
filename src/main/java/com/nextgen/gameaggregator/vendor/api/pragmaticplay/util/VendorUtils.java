package com.nextgen.gameaggregator.vendor.api.pragmaticplay.util;

import com.nextgen.gameaggregator.exception.InvalidSignatureException;
import com.nextgen.gameaggregator.util.ValidationUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.util.stream.Collectors;

@Service
public class VendorUtils extends ValidationUtils {
    public static void validateHash(String hash, String secretKey, String requestBody) throws InvalidSignatureException {
        String requestData = requestBody.replaceAll("(^|&)hash=.*?(&|$)", "$1$2");
        String generatedHash = generateHash(requestData, secretKey);
        if (!hash.equals(generatedHash)) {
            throw new InvalidSignatureException();
        }
    }

    private static String generateHash(MultiValueMap<String, String> params, String secret) {
        String payload = params.keySet().stream()
                .sorted()
                .map(key -> key + "=" + params.get(key).get(0))
                .collect(Collectors.joining("&"));

        return generateHash(payload, secret);
    }

    private static String generateHash(String payload, String secret) {
        payload += secret;
        return DigestUtils.md5Hex(payload);
    }
}
