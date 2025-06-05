package com.nextgen.gameaggregator.util;

import com.nextgen.gameaggregator.exception.InvalidDecryptionException;
import com.nextgen.gameaggregator.exception.InvalidEncryptionException;
import lombok.experimental.UtilityClass;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@UtilityClass
public class EncryptionUtils {
    private static final String ALGORITHM = "AES";

    /**
     * Encrypt a plain text string using AES algorithm with the provided secret key.
     *
     * @param cipherModeAndPadding Encryption Mode and Padding Type
     * @param plainText            The original string to encrypt
     * @param secret               The encryption key (should be 16/24/32 chars for AES-128/192/256)
     * @return Base64 encoded encrypted string
     */
    public static String aesEncrypt(String cipherModeAndPadding, String plainText, String secret)
            throws InvalidEncryptionException {
        String iv = cipherModeAndPadding.toUpperCase().contains("ECB") ? null : new String(new byte[16]);
        return aesEncrypt(cipherModeAndPadding, plainText, secret, iv);
    }

    /**
     * Encrypt a plain text string using AES algorithm with the provided secret key.
     *
     * @param cipherModeAndPadding Encryption Mode and Padding Type
     * @param plainText            The original string to encrypt
     * @param secret               The encryption key (should be 16/24/32 chars for AES-128/192/256)
     * @param iv                   IV for this encryption
     * @return Base64 encoded encrypted string
     */
    public static String aesEncrypt(String cipherModeAndPadding, String plainText, String secret, String iv)
            throws InvalidEncryptionException {
        try {
            Cipher cipher = Cipher.getInstance(cipherModeAndPadding);
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(), ALGORITHM);
            if (iv == null) {
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            } else {
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv.getBytes()));
            }
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new InvalidEncryptionException(e.toString());
        }
    }

    /**
     * Decrypt an AES-encrypted Base64 string using the provided secret key.
     *
     * @param cipherModeAndPadding Decryption Mode and Padding Type
     * @param encryptedText        The encrypted Base64 string
     * @param secret               The secret key used for decryption
     * @return Decrypted plain text string
     */
    public static String aesDecrypt(String cipherModeAndPadding, String encryptedText, String secret)
            throws InvalidDecryptionException {
        String iv = cipherModeAndPadding.toUpperCase().contains("ECB") ? null : new String(new byte[16]);
        return aesDecrypt(cipherModeAndPadding, encryptedText, secret, iv);
    }

    /**
     * Decrypt an AES-encrypted Base64 string using the provided secret key.
     *
     * @param cipherModeAndPadding Decryption Mode and Padding Type
     * @param encryptedText        The encrypted Base64 string
     * @param secret               The secret key used for decryption
     * @param iv                   IV for this decryption
     * @return Decrypted plain text string
     */
    public static String aesDecrypt(String cipherModeAndPadding, String encryptedText, String secret, String iv)
            throws InvalidDecryptionException {
        try {
            Cipher cipher = Cipher.getInstance(cipherModeAndPadding);
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(), ALGORITHM);
            if (iv == null) {
                cipher.init(Cipher.DECRYPT_MODE, secretKey);
            } else {
                cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv.getBytes()));
            }
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes);
        } catch (Exception e) {
            throw new InvalidDecryptionException(e.toString());
        }
    }


}
