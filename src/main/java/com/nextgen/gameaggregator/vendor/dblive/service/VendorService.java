package com.nextgen.gameaggregator.vendor.dblive.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.nextgen.gameaggregator.exception.InvalidSignatureException;
import com.nextgen.gameaggregator.exception.InvalidVendorLineException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;

@Service
public class VendorService extends BaseVendorService {

    public static void verifySignature(String param, String md5Key, String signature) throws InvalidSignatureException {
        String md5Signature = getMD5(param + md5Key);
        try {
            ValidationUtils.isEquals(signature, md5Signature, InvalidSignatureException::new);
        } catch (Exception e) {
            throw new InvalidSignatureException();
        }
    }

    public static String encrypt(String plainText, String key) throws InvalidVendorLineException {
        try {
            Key aesKey = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new InvalidVendorLineException(e.getMessage());
        }
    }

    public static String getMD5(String input) {
        return DigestUtils.md5Hex(input).toUpperCase();
    }

    public static <T> T convertDto(String params, Class<T> clazz) {
        Gson gson = new Gson();
        return gson.fromJson(params, clazz);
    }

    public static <T> T convertDto(Object params, Class<T> clazz) {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(params, clazz);
    }

    public static String extractVendorPlayerUsername(String loginName) {
        return loginName.substring(loginName.indexOf("_") + 1);
    }
}
