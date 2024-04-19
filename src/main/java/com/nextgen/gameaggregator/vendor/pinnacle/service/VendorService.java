package com.nextgen.gameaggregator.vendor.pinnacle.service;

import com.nextgen.gameaggregator.vendor.pinnacle.utils.Signature;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Objects;

@Slf4j
@Service
public class VendorService {
    private static final String ALGORITHM = "AES";
    private static final String INIT_VECTOR = "RandomInitVector";

    private static String encryptAES(String secretKey, String tokenPayLoad) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), ALGORITHM);
            IvParameterSpec iv = new IvParameterSpec(INIT_VECTOR.getBytes("UTF-8"));
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, iv);
            byte[] encrypted = cipher.doFinal(tokenPayLoad.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);

        } catch (Exception ex) {
            log.error("encryptAES error : ", ex);
        }

        return null;
    }

    /**
     * Descrypt String by secret_key base on ASE algorithm.
     *
     * @param encryptedText - String is return from {#link encryptAES()}
     * @param aesKey        - secrect_key be provide by platform.
     * @return String | NULL.
     */
    public static String decryptAES(String encryptedText, String aesKey) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(aesKey.getBytes(), ALGORITHM);
            IvParameterSpec iv = new IvParameterSpec(INIT_VECTOR.getBytes("UTF-8"));
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, iv);
            byte[] original = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(original);
        } catch (Exception ex) {
            log.error("encryptAES error : ", ex);
        }
        return null;
    }

    public static Boolean isCorrectVendorPlayerUsername(String vendorPlayerUsername, String prefix) {

        return vendorPlayerUsername.startsWith(prefix);
    }

    public static Long convertDateTimeStringToTimestamp(String dateTimeString, String dateTimeFormat, ZoneId zoneId) {

        if (Objects.isNull(dateTimeString) || Objects.isNull(dateTimeFormat) || Objects.isNull(zoneId))
            return System.currentTimeMillis();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateTimeFormat);
        ZonedDateTime zonedDateTime = LocalDateTime.parse(dateTimeString, formatter).atZone(zoneId);
        return zonedDateTime.toInstant().toEpochMilli();
    }

    public static String generateToken(String agentCode, String agentKey, String secretKey) {
        String sTimestamp = String.valueOf(System.currentTimeMillis());
        String hashToken = DigestUtils.md5Hex(agentCode + sTimestamp + agentKey);
        String tokenPayLoad = String.format("%s|%s|%s", agentCode, sTimestamp, hashToken);
        return encryptAES(secretKey, tokenPayLoad);
    }

    /**
     * Decode string to verify signature in request.
     *
     * @param token  - string value of singature.
     * @param aesKey - it is `secret_key` that will provided from platform.
     * @return Signature
     */
    public Signature decode(String token, String aesKey) {
        String tokenPayload = decryptAES(token, aesKey);
        String[] tmp = tokenPayload.split("\\|");
        Signature signature = new Signature(tmp[0], tmp[2], aesKey);
        signature.setTimestamp(tmp[1]);
        return signature;
    }
}
