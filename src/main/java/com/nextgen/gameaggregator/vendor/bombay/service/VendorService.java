package com.nextgen.gameaggregator.vendor.bombay.service;

import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.BaseVendorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

@Service
@Slf4j
public class VendorService extends BaseVendorService {
    private static Signature sig;

    static {
        try {
            sig = Signature.getInstance("SHA256WithRSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static String generateSignature(String string_to_sign,String base64PrivateKey) throws Exception {
        byte[] data = string_to_sign.getBytes(StandardCharsets.UTF_8);

        sig.initSign(loadPrivateKeyFromString(base64PrivateKey));
        sig.update(data);

        byte[] signatureBytes = sig.sign();
        return Base64.getEncoder().encodeToString(signatureBytes);
    }

    public static Boolean validateSignature(String signature, String string_to_validate,String base64PublicKey) throws Exception {
        byte[] string_to_validate_bytes = string_to_validate.getBytes(StandardCharsets.UTF_8);
        byte[] signature64 = Base64.getDecoder().decode(signature);

        sig.initVerify(loadPublicKeyFromString(base64PublicKey));
        sig.update(string_to_validate_bytes);

        return sig.verify(signature64);
    }

    /**
     * @author -
     * description: get public key String and convert to public key
     * @param publicKeyString: string public key
     * @return Object PublicKey
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     **/
    public static PublicKey loadPublicKeyFromString(String publicKeyString) throws Exception {
        // Remove the header and footer lines if they are present
        String publicKeyContent = publicKeyString
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("[\\r\\n\\s]", ""); // Remove all whitespace characters

        // Decode the Base64-encoded key content
        byte[] decodedKey = Base64.getDecoder().decode(publicKeyContent);

        // Generate the public key specification
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedKey);

        // Generate and return the public key
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }
    /**
     * @author -
     * description: get private key String and convert to public key
     * @param base64PrivateKey: string private key
     * @return Object PrivateKey
     * @throws  NoSuchAlgorithmException
     * @throws InvalidKeyException
     **/
    public static PrivateKey loadPrivateKeyFromString(String base64PrivateKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        // Remove the header and footer lines if they are present
        String privateKeyContent = base64PrivateKey
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s", ""); // Remove all whitespace characters

        // Decode the Base64-encoded key content
        byte[] decodedKey = Base64.getDecoder().decode(privateKeyContent);

        // Generate the private key specification
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedKey);

        // Generate and return the private key
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    public static String trimGameCode(String gameCode){

        String trimmedGameCode = null;

        // check if game code contain _stg (ignore case-sensitive)
        if(gameCode.toLowerCase().contains("_stg")){
            // Trim value by removing _stg (ignore case-sensitive)
            trimmedGameCode = gameCode.replaceFirst("(?i)_stg$", "");
        }else{
            // let trimmedCode same as gameCode
            trimmedGameCode = gameCode;
        }

        return trimmedGameCode;
    }

    public static Map<String, Object> convertToHashMap(MultiValueMap<String, String> multiValueMap) {
        Map<String, Object> hashMap = new HashMap<>();

        // Iterate over entries in the MultiValueMap
        for (Map.Entry<String, List<String>> entry : multiValueMap.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            // Convert the list of values into an Object, e.g., by selecting the first value
            Object value = (values != null && !values.isEmpty()) ? (Object) values.get(0) : null;
            hashMap.put(key, value);
        }

        return hashMap;
    }

    public static ResultType checkResult(Integer winAmount){
        ResultType resultType = null;

        if(winAmount > 0){
            resultType = ResultType.WIN;
        }else{
            resultType = ResultType.LOSE;
        }

        return resultType;
    }

    public static Map<String, String> headersToHashMap(HttpServletRequest request) {
        // Create a HashMap to store the headers
        Map<String, String> headersMap = new HashMap<>();

        // Get all header names
        Enumeration<String> headerNames = request.getHeaderNames();

        // Iterate through the header names and put them into the HashMap
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            headersMap.put(headerName, headerValue);
        }

        return headersMap;
    }
}
