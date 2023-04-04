package com.nextgen.gameaggregator.vendor.joker.service;

import com.nextgen.gameaggregator.exception.InvalidDecryptionException;
import com.nextgen.gameaggregator.exception.InvalidEncryptionException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class VendorService {
    public String aesEncrypt(String dataString, String appKey) throws InvalidEncryptionException {
        try {
            Base64.Encoder encoder = Base64.getEncoder();
            SecretKeySpec keySpec = new SecretKeySpec(appKey.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return encoder.encodeToString(cipher.doFinal(dataString.getBytes("UTF-8")));
        } catch (Exception exception){
            throw new InvalidEncryptionException();
        }
    }

    public String aesDecrypt(String dataString, String appKey) throws InvalidDecryptionException {
        try {
            Base64.Decoder decoder = Base64.getDecoder();
            SecretKeySpec keySpec = new SecretKeySpec(appKey.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            return new String(cipher.doFinal(decoder.decode(dataString)));
        } catch (Exception exception){
            throw new InvalidDecryptionException();
        }
    }

    public static String md5(String input) throws InvalidEncryptionException {
        try {
            return DigestUtils.md5Hex(input);
        } catch (Exception exception){
            throw new InvalidEncryptionException();
        }
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

    public boolean isValidInteger(Integer number) {
        // check integer not blank, not null, and not a space
        return number != null && number.toString().trim().length() > 0;
    }

    public boolean isValidTimestamp(long timestamp) {
        try {
            Instant instant = Instant.ofEpochMilli(timestamp);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isValidDateString(String timestamp, String pattern) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
        try {
            Date date = dateFormat.parse(timestamp);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String generateGameUrl(String apiUrl, MultiValueMap<String, String> parameters) {
        // form query string
        String queryString = "";
        List<String> values = new ArrayList<>();
        for (String key : parameters.keySet()){
            values.add(key + "=" + parameters.getFirst(key));
        }

        String loginUrl = apiUrl + "?" + String.join("&", values);

        return loginUrl;
    }
}
