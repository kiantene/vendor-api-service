package com.nextgen.gameaggregator.vendor.ezugi.service;

import com.nextgen.gameaggregator.exception.InvalidSignatureException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Service
@Slf4j
public class VendorService extends BaseVendorService {
    public static String generateGameUrl(String lobbyUrl, String playerGameSessionToken, String operatorId, String languageCode, String gameCode) {
        // form query string
        String loginUrl = lobbyUrl + "?token=" + playerGameSessionToken + "&operatorId=" + operatorId + "&language=" + languageCode + "&openTable=" + gameCode;
        return loginUrl;
    }

    public static void verifyHash(String secretKey, String data, String hashKey) throws InvalidKeyException, NoSuchAlgorithmException, InvalidSignatureException {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(secretKey.getBytes(),
                "HmacSHA256");
        sha256_HMAC.init(secret_key);
        String generatedHash = Base64.encodeBase64String(sha256_HMAC.doFinal(data.getBytes()));
        if (!hashKey.equals(generatedHash)) {
            String msg = "Expected hash: " + generatedHash + ", but received: " + hashKey + " Key: "+secretKey;
            log.error("Request body: " + data);
            log.error(msg);
            throw new InvalidSignatureException(msg);
        }
    }
}
