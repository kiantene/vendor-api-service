package com.nextgen.gameaggregator.vendor.iloveu.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgen.gameaggregator.exception.InvalidEncryptionException;
import com.nextgen.gameaggregator.service.BaseVendorService;
import com.nextgen.gameaggregator.vendor.iloveu.api.settle.SettleTransactionDto;
import com.nextgen.gameaggregator.vendor.iloveu.constant.Formats;
import com.nextgen.gameaggregator.vendor.iloveu.vo.CommonVo;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(Formats.DATE_FORMAT);

        try {
            LocalDateTime.parse(dateTimeString, dateFormat);
            return true; // Parsing succeeded, so the format is valid
        } catch (Exception e) {
            return false; // Parsing failed, so the format is invalid
        }
    }

    public static Long dateTimeConvert(String rawDateTime) {

        //convert date time string to timestamp
        Long timestamp = null;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Formats.DATE_FORMAT);
        return ZonedDateTime.of(LocalDateTime.parse(rawDateTime, formatter), ZoneId.of(Formats.TIME_ZONE)).toInstant().toEpochMilli();

    }

    public static <T> T convertJsonToDto(String json, Class<T> objectClass) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        return mapper.readValue(json, objectClass);
    }

    public static List<CommonVo> processMultipleDataResponds(List<CompletableFuture<CommonVo>> bets) {

        CompletableFuture<Void> allBets = CompletableFuture.allOf(bets.toArray(new CompletableFuture[bets.size()]));
        allBets.join();
        List<CommonVo> transactionsList = bets.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
        return transactionsList;
    }

}
