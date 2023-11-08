package com.nextgen.gameaggregator.vendor.pinnacle.service;

import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class VendorService {
    private static final String ALGORITHM = "AES";
    private static final String INIT_VECTOR = "RandomInitVector";
    public String generateToken(String agentCode, String agentKey, String secretKey) throws NoSuchAlgorithmException { 
        String sTimestamp = String.valueOf(System.currentTimeMillis());
        String hashToken = DigestUtils.md5Hex(agentCode + sTimestamp + agentKey);
        String tokenPayLoad = String.format("%s|%s|%s", agentCode, sTimestamp, hashToken);
        String token = encryptAES(secretKey, tokenPayLoad);
        return token;    
    }    
    
    private static String encryptAES(String secretKey, String tokenPayLoad) {  
        try {  
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(), ALGORITHM);
            IvParameterSpec iv = new IvParameterSpec(INIT_VECTOR.getBytes("UTF-8"));
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, iv);
            byte[] encrypted = cipher.doFinal(tokenPayLoad.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);

        } catch (Exception ex) {
            log.error("encryptAES error : ", ex);
        }

        return null;  
    }
}
