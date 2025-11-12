package com.nextgen.gameaggregator.vendor.wmlive.service;

import com.nextgen.gameaggregator.service.BaseVendorService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Service
public class VendorService extends BaseVendorService {
    public static Long convertDateTimeStringToTimestamp(String dateTimeString, String dateTimeFormat) {
        ZoneId zoneId = ZoneId.of("GMT+8");
        if (Objects.isNull(dateTimeString)) return System.currentTimeMillis();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateTimeFormat);
        ZonedDateTime zonedDateTime = LocalDateTime.parse(dateTimeString, formatter).atZone(zoneId);
        return zonedDateTime.toInstant().toEpochMilli();
    }

}
