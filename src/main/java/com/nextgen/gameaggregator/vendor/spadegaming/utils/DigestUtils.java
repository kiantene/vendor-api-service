package com.nextgen.gameaggregator.vendor.spadegaming.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;

public abstract class DigestUtils {
    public static String digest(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input);
            return Hex.encodeHexString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String digest(byte[] input, byte[] secretKey) {
        int bodySize = input.length;
        int keySize = secretKey.length;
        byte[] buffer = new byte[bodySize + keySize];
        System.arraycopy(input, 0, buffer, 0, bodySize);
        System.arraycopy(secretKey, 0, buffer, bodySize, keySize);
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(buffer);
            return Hex.encodeHexString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
