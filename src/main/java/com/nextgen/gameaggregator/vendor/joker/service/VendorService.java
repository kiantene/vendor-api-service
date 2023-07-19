package com.nextgen.gameaggregator.vendor.joker.service;

import com.nextgen.gameaggregator.exception.InvalidSignatureException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class VendorService extends BaseVendorService {

    public static String generateHash(MultiValueMap<String, String> params, String secret) {
        String payload = params.keySet().stream().sorted()
                .map(key -> key.toLowerCase() + "=" + params.get(key).get(0))
                .collect(Collectors.joining("&"));

        return generateHash(payload, secret);
    }

    public static String generateHash(Map<String, String> params, String secret) {
        String payload = params.keySet().stream().sorted()
                .map(key -> key.toLowerCase() + "=" + params.get(key))
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
            if (kv.length == 2) {
                if(kv[0].equals("amount")){
                    //fix amount into 2 decimal
                    BigDecimal amount = new BigDecimal(kv[1]);
                    kv[1] = Double.toString(amount.setScale(2, RoundingMode.DOWN).doubleValue());
                }
                queryParameterMap.put(kv[0], kv[1]);
            }
        }

        return queryParameterMap;
    }

    public static void verifyHash(String requestBody, String secretKey) throws InvalidSignatureException {
        try {
            // Url decoder for the request body
            requestBody = URLDecoder.decode(requestBody, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new InvalidSignatureException();
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

    public static String generateGameUrl(String apiUrl, MultiValueMap<String, String> parameters) {
        // form query string
        String queryString = "";
        List<String> values = new ArrayList<>();
        for (String key : parameters.keySet()){
            values.add(key + "=" + parameters.getFirst(key));
        }

        String loginUrl = apiUrl + "?" + String.join("&", values);

        return loginUrl;
    }
}
