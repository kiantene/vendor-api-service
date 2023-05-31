package com.nextgen.gameaggregator.vendor.ezugi.service;

import com.nextgen.gameaggregator.exception.InvalidSignatureException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

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
            String msg = "Expected hash: " + generatedHash + ", but received: " + hashKey;
            log.error("Request body: " + data);
            log.error(msg);
            throw new InvalidSignatureException(msg);
        }
    }

    public static String generateRequestToken(MultiValueMap<String, String> params) throws NoSuchAlgorithmException {
        List<String> values = new ArrayList<>();
        for (String key : params.keySet()){
            values.add(key + "=" + params.getFirst(key));
        }
        String queryString = String.join("&", values);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(queryString.getBytes());
        return hash.toString();
    }
}
