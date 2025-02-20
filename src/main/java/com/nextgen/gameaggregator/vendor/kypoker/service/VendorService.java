package com.nextgen.gameaggregator.vendor.kypoker.service;


import com.nextgen.gameaggregator.exception.InvalidEncryptionException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import lombok.Data;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;


@Data
@Service
public class VendorService extends BaseVendorService {

    public static String aesEncrypt(String dataString, String appKey) throws InvalidEncryptionException {
        try {
            java.util.Base64.Encoder encoder = java.util.Base64.getEncoder();
            SecretKeySpec keySpec = new SecretKeySpec(appKey.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return encoder.encodeToString(cipher.doFinal(dataString.getBytes("UTF-8")));
        } catch (Exception exception) {
            throw new InvalidEncryptionException();
        }
    }

    public static String AESDecrypt(String value,String key,boolean isDecodeURL) throws Exception {
        try {
            byte[] raw = key.getBytes("UTF-8");
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            if(isDecodeURL)	value = URLDecoder.decode(value, "UTF-8");
            byte[] encrypted1 = Base64.decode(value);// 先用base64解密
            byte[] original = cipher.doFinal(encrypted1);
            String originalString = new String(original, "UTF-8");
            return originalString;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static String MD5Encrypt(String sourceStr) {
        String result = "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(sourceStr.getBytes("UTF-8"));
            byte b[] = md.digest();
            int i;
            StringBuffer buf = new StringBuffer("");
            for (int offset = 0; offset < b.length; offset++) {
                i = b[offset];
                if (i < 0)
                    i += 256;
                if (i < 16)
                    buf.append("0");
                buf.append(Integer.toHexString(i));
            }
            result = buf.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;
    }

}
