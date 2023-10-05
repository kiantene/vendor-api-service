package com.nextgen.gameaggregator.vendor.yesbingo.service;

import com.nextgen.gameaggregator.service.BaseVendorService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;

@Service
@Slf4j
@Data
public class VendorService extends BaseVendorService {

    public static String encrypt(String str, String key, String iv)
            throws
            NoSuchPaddingException,
            NoSuchAlgorithmException,
            InvalidAlgorithmParameterException,
            InvalidKeyException,
            IllegalBlockSizeException,
            BadPaddingException {

        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

        byte[] encrypted = cipher.doFinal(padString(str).getBytes(StandardCharsets.UTF_8));
        String encoded = Base64.getEncoder().encodeToString(encrypted);

        return encoded.replace("+", "-").replace("/", "_").replace("=", "");

    }

    public static String decrypt(String code, String key, String iv)
            throws
            NoSuchPaddingException,
            NoSuchAlgorithmException,
            InvalidAlgorithmParameterException,
            InvalidKeyException,
            IllegalBlockSizeException,
            BadPaddingException {

        code = code.replace('-', '+').replace('_', '/');
        byte[] decodedBytes = Base64.getDecoder().decode(code);
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);

        return new String(decryptedBytes, StandardCharsets.UTF_8).trim();

    }

    private static String padString(String str) {
        int blockSize = 16;
        int padSize = blockSize - (str.length() % blockSize);
        char padChar = (char) padSize;
        StringBuilder padded = new StringBuilder(str);
        padded.append(String.valueOf(padChar).repeat(padSize));
        return padded.toString();

    }

    public static String getSign(String data) {
        String token = DigestUtils.md5Hex(data);
        return token.toUpperCase();

    }

    public static Long getCurrentTime(String date) {
        return Instant.parse(date).toEpochMilli();
    }

}
