package com.nextgen.gameaggregator.vendor.facai.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Service
@Slf4j
public class VendorService {
    public String aesEncrypt(String dataString, String appKey) throws Exception {
        Base64.Encoder encoder = Base64.getEncoder();
        SecretKeySpec keySpec = new SecretKeySpec(appKey.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        return encoder.encodeToString(cipher.doFinal(dataString.getBytes("UTF-8")));
    }

    public String aesDecrypt(String dataString, String appKey) throws Exception {
        Base64.Decoder decoder = Base64.getDecoder();
        SecretKeySpec keySpec = new SecretKeySpec(appKey.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance( "AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        return new String(cipher.doFinal(decoder.decode(dataString)));
    }

    public static String md5(String input) throws Exception {
        return DigestUtils.md5Hex(input);
    }

    public boolean isValidString(String str) {
        // Not Chinese characters, not special characters (~!@#$%^&*()+) except underscore and no spacing
        String pattern = "^[^\\p{InCJKUnifiedIdeographs}\\s~!@#$%^&*()+]*$";
        String patternNoSpace = "^[^\\s]*$";
        return str != null && !str.isBlank() && str.matches(pattern) && str.matches(patternNoSpace);
    }

    public boolean isValidStringLength(String str, Integer min, Integer max) {
        //check string length
        int length = str.length();

        if(str.length() >= min){
            //skip max length check if max =  0
            if(max == 0){
                return true;
            } else if (str.length() <= max) {
                return true;
            }else{
                return false;
            }
        }else{
            return false;
        }

    }

    public boolean isTimestamp(long value) {
        // Check if value is within valid timestamp range
        long minTimestamp = -62135596800000L; // January 1, 0001 00:00:00 UTC
        long maxTimestamp = 253402300799999L; // December 31, 9999 23:59:59 UTC
        return value >= minTimestamp && value <= maxTimestamp;
    }

}
