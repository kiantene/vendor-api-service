package com.nextgen.gameaggregator.vendor.habanero.service;

import com.nextgen.gameaggregator.exception.InvalidEncryptionException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class VendorService extends BaseVendorService {

    public static String generateUrl(String apiUrl, MultiValueMap<String, String> parameters) {
        // form query string
        String queryString = "";
        List<String> values = new ArrayList<>();
        for (String key : parameters.keySet()){
            values.add(key + "=" + parameters.getFirst(key));
        }

        String loginUrl = apiUrl + "?" + String.join("&", values);

        return loginUrl;
    }

    public static String generateSHA256Hash(String input) throws InvalidEncryptionException {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = messageDigest.digest(input.getBytes());
            StringBuilder stringBuilder = new StringBuilder();

            for (byte hashByte : hashBytes) {
                stringBuilder.append(String.format("%02x", hashByte));
            }

            return stringBuilder.toString();
        } catch (Exception exception) {
            throw new InvalidEncryptionException();
        }
    }

    public static boolean isValidDateString(String timestamp) {
        try {
            OffsetDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

}
