package com.nextgen.gameaggregator.core.signature;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.DigestUtils;

public class Md5SignatureStrategy implements SignatureStrategy {
    public enum ConcatenationOrder {
        PAYLOAD_SECRET,
        SECRET_PAYLOAD
    }

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcatenationOrder order;

    public Md5SignatureStrategy() {
        this(ConcatenationOrder.PAYLOAD_SECRET);
    }

    public Md5SignatureStrategy(ConcatenationOrder order) {
        this.order = order;
    }

    @Override
    public String sign(String payload, String secret) {
        return order == ConcatenationOrder.PAYLOAD_SECRET
                ? DigestUtils.md5Hex(payload + secret)
                : DigestUtils.md5Hex(secret + payload);
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
