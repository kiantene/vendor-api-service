package com.nextgen.gameaggregator.vendor.mtlive.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.core.exception.InternalServerException;
import com.nextgen.gameaggregator.core.util.VendorCredentialAccessor;
import com.nextgen.gameaggregator.vendor.mtlive.constant.Credentials;
import com.nextgen.gameaggregator.vendor.mtlive.constant.Headers;
import com.nextgen.gameaggregator.vendor.mtlive.response.SuccessResponse;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.http.ResponseEntity;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

public class VendorUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String encrypt(String plainText, String key, String iv) throws Exception {
        Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "DES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes());

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public static ResponseEntity<String> encryptResponse(Object response, VendorCredentialAccessor accessor) {
        try {
            long timeStamp = Instant.now().getEpochSecond();
            if (response instanceof SuccessResponse success) {
                success.setTimestamp(timeStamp);
            }
            String clientSecret = accessor.getValue(Credentials.CLIENT_SECRET);
            String clientId = accessor.getValue(Credentials.CLIENT_ID);
            String key = accessor.getValue(Credentials.DES_KEY);
            String iv  = accessor.getValue(Credentials.DES_IV);
            String encryptedResponse;
            try {
                encryptedResponse = encrypt(objectMapper.writeValueAsString(response), key, iv);
            } catch (Exception e) {
                throw new InternalServerException();
            }
            String signature = DigestUtils.md5Hex(timeStamp+clientSecret+clientId+encryptedResponse);

            // 3. Build ResponseEntity
            return ResponseEntity.ok()
                    .header("Content-Type", "text/plain")
                    .header(Headers.API_CI, clientId)
                    .header(Headers.API_SI, signature)
                    .header(Headers.API_TS, String.valueOf(timeStamp))
                    .body(encryptedResponse);

        } catch (Exception e) {
            throw new InternalServerException("Failed to encrypt MT Live response", e);
        }
    }
}
