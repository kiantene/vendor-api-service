package com.nextgen.gameaggregator.controller;

import com.nextgen.gameaggregator.entity.VendorLine;
import com.nextgen.gameaggregator.service.VendorLineService;
import com.nextgen.gameaggregator.vendor.pgsoft.constant.Credentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(path = "health/")
@Slf4j
public class HealthCheckController {
    @Autowired
    private VendorLineService vendorLineService;
    @Value("${mavenTimestamp}")
    private String timestamp;

    @Value("${spring.profiles.active}")
    private String profilesActive;

    @GetMapping(path = "status")
    public String status() {
        log.info("Health Check OK");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.of("UTC"));
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(timestamp, formatter);
        zonedDateTime = zonedDateTime.withZoneSameInstant(ZoneId.of("Asia/Singapore"));
        String timezoneTimestamp = zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd K:mm:ssa z"));

        return "OK" + " | VENDOR | " + profilesActive + " | " + timezoneTimestamp;
    }

    @GetMapping(path = "db")
    public String db() {

        long startTime = 0, endTime = 0, time = 0;
        try {
            startTime = System.currentTimeMillis();

            VendorLine vendorLine = vendorLineService.getVendorLineByAgent(2, 2, 2);

            endTime = System.currentTimeMillis();
            time = endTime - startTime;

            log.info("DB Check Latency: {}", time);

        } catch (Exception exception) {
            log.info(exception.toString());
        }
        return "DB Check Latency: " + String.valueOf(time);
    }


    @GetMapping(path = "redis")
    public String redis() {
        long startTime = 0, endTime = 0, time = 0;

        try {
            startTime = System.currentTimeMillis();

            String secretKey = vendorLineService.getCredentialValueByName(2, "secretKey");

            endTime = System.currentTimeMillis();
            time = endTime - startTime;

            log.info("Redis Check Latency {}", time);

        } catch (Exception exception) {
            log.info(exception.toString());
        }

        return "Redis Check Latency: " + String.valueOf(time);
    }
}
