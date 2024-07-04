package com.nextgen.gameaggregator.vendor.saba.service;

import com.nextgen.gameaggregator.entity.ga.VendorLanguageCode;
import com.nextgen.gameaggregator.vendor.saba.api.betdetail.LangNameDto;
import com.nextgen.gameaggregator.vendor.saba.constant.EndPoints;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class VendorService {

    public static Long convertToUnixTimestamp(String dateTimeString, String datePattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(datePattern);
        try {
            return sdf.parse(dateTimeString).getTime();
        } catch (Exception e) {
            return null;
        }
    }

    public static String getNameByLang(VendorLanguageCode vendorLanguageCode, List<LangNameDto> list) {

        Map<String, String> languageMap = list.stream()
                .collect(Collectors.toMap(LangNameDto::getLang, LangNameDto::getName));
        return languageMap.getOrDefault(vendorLanguageCode.getLanguageCode(), list.get(0).getLang());

    }

    public static String generateMultipleBetRoundId(List<String> refIdList) {
        Collections.sort(refIdList);
        return DigestUtils.md5Hex(String.join("&", refIdList));
    }

    public String convertDateTimeFormat(Long UnixTimestamp) {

        // Convert milliseconds to Instant
        Instant instant = Instant.ofEpochMilli(UnixTimestamp);

        // Define the GMT-4 time zone
        ZoneId gmtMinus4 = ZoneId.of("GMT-4");

        // Convert Instant to ZonedDateTime with the GMT-4 time zone
        ZonedDateTime zonedDateTime = instant.atZone(gmtMinus4);

        // Define the format for ISO8601
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

        // Format the ZonedDateTime to ISO8601 format
        String iso8601WithOffset = zonedDateTime.format(formatter);

        return iso8601WithOffset;
    }

    public String generateBatchProcessId(String action, String operationId) {
        String idempotentId = EndPoints.VENDOR_CODE + "_" + action + "_" + operationId;
        idempotentId = DigestUtils.md5Hex(idempotentId).toUpperCase();

        return idempotentId;
    }

    public static String generateExtTxnId(String operationId, String refId) {
        return operationId + "-" + refId;
    }
}
