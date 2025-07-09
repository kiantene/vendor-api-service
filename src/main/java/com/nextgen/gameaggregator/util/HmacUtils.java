package com.nextgen.gameaggregator.util;

import lombok.experimental.UtilityClass;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@UtilityClass
public class HmacUtils {
    /**
     * Generates an HMAC hash.
     *
     * @param algorithm   The HMAC algorithm (e.g., "HmacSHA256", "HmacSHA1", "HmacSHA512").
     * @param apiSecret   The secret key.
     * @param requestBody The message body as a byte array.
     * @return The computed HMAC hash as a hexadecimal string.
     */
    public static String generate(String algorithm, String apiSecret, byte[] requestBody) {
        try {
            byte[] secretBytes = apiSecret.getBytes(StandardCharsets.UTF_8);

            Mac mac = Mac.getInstance(algorithm);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretBytes, algorithm);
            mac.init(secretKeySpec);

            byte[] hashMessage = mac.doFinal(requestBody);

            return bytesToHex(hashMessage);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error while generating HMAC hash with algorithm: " + algorithm, e);
        }
    }

    /**
     * Generates an HMAC hash for a string message.
     *
     * @param algorithm The HMAC algorithm.
     * @param apiSecret The secret key.
     * @param message   The message to hash.
     * @return The computed HMAC hash as a hexadecimal string.
     */
    public static String generate(String algorithm, String apiSecret, String message) {
        return generate(algorithm, apiSecret, message.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Converts a byte array to a hexadecimal string.
     *
     * @param bytes The byte array to convert.
     * @return The corresponding hexadecimal string.
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }
}
