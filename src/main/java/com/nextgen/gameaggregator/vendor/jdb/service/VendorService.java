package com.nextgen.gameaggregator.vendor.jdb.service;

import com.nextgen.gameaggregator.exception.InvalidDecryptionException;
import com.nextgen.gameaggregator.exception.InvalidEncryptionException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

@Service
@Slf4j
public class VendorService {
    public static String encrypt(String data, String key, String iv) throws InvalidEncryptionException {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            int blockSize = cipher.getBlockSize();
            byte[] dataBytes = data.getBytes("UTF-8");
            int plainTextLength = dataBytes.length;
            if (plainTextLength % blockSize != 0) {
                plainTextLength = plainTextLength + (blockSize - plainTextLength % blockSize);
            }
            byte[] plaintext = new byte[plainTextLength];
            System.arraycopy(dataBytes, 0, plaintext, 0, dataBytes.length);
            SecretKeySpec keyspec = new SecretKeySpec(key.getBytes(), "AES");
            IvParameterSpec ivspec = new IvParameterSpec(iv.getBytes());
            cipher.init(Cipher.ENCRYPT_MODE, keyspec, ivspec);
            byte[] encrypted = cipher.doFinal(plaintext);
            return Base64.encodeBase64URLSafeString(encrypted);
        } catch (Exception exception) {
            throw new InvalidEncryptionException(exception.getMessage());
        }
    }

    public static String decrypt(String data, String key, String iv) throws InvalidDecryptionException {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key.getBytes(), "AES"), new IvParameterSpec(iv.getBytes()));
            String decryptData = new String(cipher.doFinal(Base64.decodeBase64(data)));
            return decryptData;
        } catch (Exception exception) {
            throw new InvalidDecryptionException(exception.getMessage());
        }
    }
}
