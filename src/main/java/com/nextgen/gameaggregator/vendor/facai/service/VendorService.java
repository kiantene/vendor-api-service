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

}
