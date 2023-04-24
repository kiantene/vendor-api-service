package com.nextgen.gameaggregator.vendor.facai.service;

import com.nextgen.gameaggregator.exception.InvalidDecryptionException;
import com.nextgen.gameaggregator.exception.InvalidEncryptionException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

@Service
@Slf4j
public class VendorService extends BaseVendorService {
    public String aesEncrypt(String dataString, String appKey) throws InvalidEncryptionException {
        try {
            Base64.Encoder encoder = Base64.getEncoder();
            SecretKeySpec keySpec = new SecretKeySpec(appKey.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return encoder.encodeToString(cipher.doFinal(dataString.getBytes("UTF-8")));
        } catch (Exception exception) {
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
        } catch (Exception exception) {
            throw new InvalidDecryptionException();
        }
    }

    public static String md5(String input) throws InvalidEncryptionException {
        try {
            return DigestUtils.md5Hex(input);
        } catch (Exception exception) {
            throw new InvalidEncryptionException();
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

}
