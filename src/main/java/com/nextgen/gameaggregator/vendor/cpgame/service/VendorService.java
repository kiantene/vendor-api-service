package com.nextgen.gameaggregator.vendor.cpgame.service;

import com.nextgen.gameaggregator.exception.InvalidSignatureException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.util.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
@Slf4j
public class VendorService extends BaseVendorService {
    public static Map<String, Object> convertMultiValueMapToHashMap(MultiValueMap<String, String> multiValueMap) {
        Map<String, Object> hashMap = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : multiValueMap.entrySet()) {
            List<String> values = entry.getValue();
            if (values.size() == 1) {
                hashMap.put(entry.getKey(), values.get(0)); // If only one value, store as String
            } else {
                hashMap.put(entry.getKey(), new ArrayList<>(values)); // If multiple values, store as List<String>
            }
        }
        return hashMap;
    }

    public static String mapToQueryString(Map<String, Object> map) throws UnsupportedEncodingException {
        // Create a sorted set of keys
        TreeSet<String> sortedKeys = new TreeSet<>(map.keySet());

        // Build the query string
        StringBuilder queryStringBuilder = new StringBuilder();
        for (String key : sortedKeys) {
            if (queryStringBuilder.length() > 0) {
                queryStringBuilder.append("&");
            }
            try {
                String encodedKey = URLEncoder.encode(key, "UTF-8");
                String encodedValue = URLEncoder.encode(String.valueOf(map.get(key)), "UTF-8");
                queryStringBuilder.append(encodedKey)
                        .append("=")
                        .append(encodedValue);
            } catch (UnsupportedEncodingException e) {
                throw new UnsupportedEncodingException();
            }
        }
        return queryStringBuilder.toString();
    }

    public static String md5Hash(String queryString) throws NoSuchAlgorithmException {
        // Create MD5 digest instance
        MessageDigest md5Digest = MessageDigest.getInstance("MD5");

        // Update the digest with the input bytes
        md5Digest.update(queryString.getBytes());

        // Get the MD5 hash bytes
        byte[] md5HashBytes = md5Digest.digest();

        // Convert byte array to hex string
        StringBuilder hexStringBuilder = new StringBuilder();
        for (byte b : md5HashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexStringBuilder.append('0');
            }
            hexStringBuilder.append(hex);
        }

        return hexStringBuilder.toString();
    }

    public static String sha1Hash(String md5Value) throws NoSuchAlgorithmException {
        // Create SHA-1 digest instance
        MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");

        // Update the digest with the input bytes
        sha1Digest.update(md5Value.getBytes());

        // Get the SHA-1 hash bytes
        byte[] sha1HashBytes = sha1Digest.digest();

        // Convert byte array to hex string
        StringBuilder hexStringBuilder = new StringBuilder();
        for (byte b : sha1HashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexStringBuilder.append('0');
            }
            hexStringBuilder.append(hex);
        }

        return hexStringBuilder.toString();
    }

    public static String generateToken(Map<String, Object> map, String secretKey) {
        try {
            // convert hashMap into query string
            String queryString = mapToQueryString(map);

            queryString += "&secret=" + secretKey;

            // hash query string into md5 format
            String md5Value = md5Hash(queryString);

            // hash md5 value with sha1 format
            return sha1Hash(md5Value).toUpperCase();
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    public static void verifyHash(String oriRequest, String hash, String secretKey) throws InvalidSignatureException {

        try {
            int tokenIndex = oriRequest.indexOf("&token");

            // Extract the substring before "&token"
            String afterSubstring = oriRequest.substring(0, tokenIndex);

            afterSubstring += "&secret=" + secretKey;

            // hash query string into md5 format
            String expectedHash = sha1Hash(md5Hash(afterSubstring)).toUpperCase();

            ValidationUtils.isEquals(expectedHash, hash, InvalidSignatureException::new);

        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new RuntimeException();
        }
    }

}
