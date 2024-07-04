package com.nextgen.gameaggregator.vendor.ambslot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.exception.InvalidRequestException;
import com.nextgen.gameaggregator.exception.InvalidSignatureException;
import com.nextgen.gameaggregator.operator.enums.ResultType;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.vendor.ambslot.constant.EndPoints;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

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

    public static String encryption(String jsonString, String vendorSecrect, int iterations){
        try{
            // Convert the jsonString to bytes
            byte[] passwordBytes = jsonString.getBytes(StandardCharsets.UTF_8);

            // Generate a salted hash using PBKDF2 with SHA-512
            return generatePBKDF2Hash(passwordBytes, vendorSecrect, iterations, 64);
        }catch(Exception e){
            return null;
        }
    }

    public static void validateSignature(String signature, String requestBody, String secret)
            throws JsonProcessingException, InvalidRequestException, InvalidSignatureException {

        if (signature == null) throw new InvalidRequestException("Missing " + EndPoints.HEADER_SIGNATURE + " in header");

        int iterations = 1000;
        String json = convertObjectMapper(requestBody);
        String expectedSignature = encryption(json, secret, iterations);

        if (!signature.equals(expectedSignature)) {
            throw new InvalidSignatureException();
        }
    }

    public static long convertDateTimeToUnix(String dateTimeString) {
        return Instant.parse(dateTimeString).toEpochMilli();
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
        return formatter.format(zonedDateTime);
    }

    public static ResultType generateResultType(BigDecimal amount){

        if(amount.compareTo(BigDecimal.ZERO) > 0){
            return ResultType.WIN;
        }

        return ResultType.END;
    }

    public static String convertObjectMapper(String body) throws JsonProcessingException {
        // Create ObjectMapper instance
         return new ObjectMapper().readTree(body).toString();
    }

    @Override
    public boolean shouldRejectCancelRequest() {
        return false;
    }
}
