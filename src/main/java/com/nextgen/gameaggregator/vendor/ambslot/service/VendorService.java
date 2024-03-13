package com.nextgen.gameaggregator.vendor.ambslot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.BaseVendorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class VendorService extends BaseVendorService {

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

    public static String convertMapToJson(MultiValueMap<String, String> dataMap){
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(dataMap.toSingleValueMap());
        } catch (Exception e) {
            return null;
        }
    }

    // Generate signed with RSA-SHA512 and encoded to BASE64 from request body
    private static String generatePBKDF2Hash(byte[] password, String salt, int iterations, int keyLength)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        char[] passwordChars = new String(password, StandardCharsets.UTF_8).toCharArray();
        byte[] saltBytes = salt.getBytes(StandardCharsets.UTF_8);

        PBEKeySpec spec = new PBEKeySpec(passwordChars, saltBytes, iterations, keyLength * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
        byte[] hashBytes = skf.generateSecret(spec).getEncoded();

        return Base64.getEncoder().encodeToString(hashBytes);
    }

    public static String encryption(String jsonString, String vendor_secrect, int iterations){
        try{
            // Convert the jsonString to bytes
            byte[] passwordBytes = jsonString.getBytes(StandardCharsets.UTF_8);

            // Generate a salted hash using PBKDF2 with SHA-512
            String hash = generatePBKDF2Hash(passwordBytes, vendor_secrect, iterations, 64);

            return hash;
        }catch(Exception e){
            return null;
        }
    }

    public static long convertDateTimeToUnix(String dateTimeString) {

        long millisecondsSinceEpoch = Instant.parse(dateTimeString).toEpochMilli();

        return millisecondsSinceEpoch;
    }

    public static String convertUnixToDateTime(long unixTimestampMillis){
        // Convert Unix timestamp with milliseconds to Instant
        Instant instant = Instant.ofEpochMilli(unixTimestampMillis);

        // Define the time zone (GMT+8)
        ZoneId zoneId = ZoneId.of("GMT+8");

        // Convert Instant to ZonedDateTime in the desired time zone
        ZonedDateTime zonedDateTime = instant.atZone(zoneId);

        // Define the date-time format with 'Z' to indicate UTC
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        // Format ZonedDateTime to a date-time string
        String formattedDateTime = formatter.format(zonedDateTime);

        return formattedDateTime;
    }

    public static ResultType generateResultType(Double amount, Boolean endround){
        // filter endround


        if(amount > 0){
            return ResultType.WIN;
        }else{
            if(endround){
                return ResultType.END;
            }

            return ResultType.LOSE;
        }
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
