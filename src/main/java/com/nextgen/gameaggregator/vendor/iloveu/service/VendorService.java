package com.nextgen.gameaggregator.vendor.iloveu.service;

import com.nextgen.gameaggregator.exception.InvalidEncryptionException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import jakarta.xml.bind.DatatypeConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class VendorService extends BaseVendorService {

    public static String md5(String input) throws InvalidEncryptionException {
        try {
            return DigestUtils.md5Hex(input);
        } catch (Exception exception) {
            throw new InvalidEncryptionException();
        }
    }

    public static String base64(String input) throws InvalidEncryptionException {
        try {
            byte [] data = input.getBytes("utf-8");
            return DatatypeConverter.printBase64Binary(data);
        } catch (Exception exception) {
            throw new InvalidEncryptionException();
        }
    }

    public static Map<String, String> invalidRequestRespond(String respondCode) {
        Map<String, String> validation = new HashMap<>(){{
            put("0", respondCode);
        }};
        return validation;
    }

    public static boolean isValidDateTime(String dateTimeString) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        dateFormat.setLenient(false); // Disallow lenient parsing

        try {
            dateFormat.parse(dateTimeString);
            return true; // Parsing succeeded, so the format is valid
        } catch (ParseException e) {
            return false; // Parsing failed, so the format is invalid
        }
    }

    public static Long dateTimeConvert(String rawDateTime) {

        //convert date time string to timestamp
        Long timestamp = null;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        LocalDateTime localDateTime = LocalDateTime.parse(rawDateTime, formatter);
        ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, ZoneId.of("UTC+8"));
        timestamp = zonedDateTime.toInstant().toEpochMilli();

        return timestamp;

    }

}
