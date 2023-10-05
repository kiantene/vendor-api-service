package com.nextgen.gameaggregator.vendor.saba.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class VendorService {

    public String convertDateTimeFormat(Long UnixTimestamp){

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
}
