package com.nextgen.gameaggregator.vendor.crystal.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.service.BaseVendorService;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Getter
@Setter
@Service
public class VendorService extends BaseVendorService {

    public static String hashHMACSha256(String data, String secret) {
        try {
            byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hash = sha256Hmac.doFinal(dataBytes);
            return Hex.encodeHexString(hash);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static String convertToCompactJson(MultiValueMap<String, String> formData) {
        Map<String, String> dataMap = formData.toSingleValueMap();
        try {
            return new ObjectMapper().writeValueAsString(dataMap)
                    .replaceAll("\\s+", "");
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
