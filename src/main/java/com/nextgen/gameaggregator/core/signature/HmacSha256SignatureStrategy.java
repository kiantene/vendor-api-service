package com.nextgen.gameaggregator.core.signature;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class HmacSha256SignatureStrategy implements SignatureStrategy {
    private static final String HMAC_ALGO = "HmacSHA256";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EncodingType encoding;

    public HmacSha256SignatureStrategy() {
        this(EncodingType.HEX); // Keep existing behavior as default
    }

    public HmacSha256SignatureStrategy(EncodingType encoding) {
        this.encoding = encoding;
    }

    @Override
    public String sign(String rawPayload, String secret) {
        try {
            byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            byte[] dataBytes = rawPayload.getBytes(StandardCharsets.UTF_8);

            Mac sha256Hmac = Mac.getInstance(HMAC_ALGO);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, HMAC_ALGO);
            sha256Hmac.init(secretKey);

            byte[] hash = sha256Hmac.doFinal(dataBytes);
            return encodeHash(hash);
        } catch (Exception e) {
            throw new IllegalArgumentException("HMAC-SHA256 signing failed", e);
        }
    }

    @Override
    public String sign(Object payload, String secret) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            return sign(json, secret); // delegate to string version
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize payload for HMAC signing", e);
        }
    }

    private String encodeHash(byte[] hash) {
        return switch (encoding) {
            case BASE64 -> Base64.encodeBase64String(hash);
            case HEX -> Hex.encodeHexString(hash);
        };
    }

    public enum EncodingType {
        BASE64,
        HEX
    }
}
