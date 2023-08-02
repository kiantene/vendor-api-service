package com.nextgen.gameaggregator.vendor.queenmaker.service;

import com.nextgen.gameaggregator.service.BaseVendorService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
@Data
public class VendorService extends BaseVendorService {

    public static Long convertToTimestamp(String dateTimeString) {
        // Parse the date-time string to an Instant
        Instant instant = Instant.parse(dateTimeString);

        // Convert to milliseconds since the epoch
        return instant.toEpochMilli();
    }

    public static String[] splitGameCode(String vendorGameCode) {
        return vendorGameCode.split("_", 2);
    }

    public static String mergeGameCode(String prefix, String suffix) {
        return prefix + "_" + suffix;
    }

}
