package com.nextgen.gameaggregator.core.signature;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.DigestUtils;

public class Md5SignatureStrategy implements SignatureStrategy {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String sign(String payload, String secret) {
        return DigestUtils.md5Hex(payload + secret);
    }

    @Override
    public String sign(Object payload, String secret) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            return sign(json, secret);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize payload", e);
        }
    }
}
