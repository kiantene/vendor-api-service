package com.nextgen.gameaggregator.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping(path = "health/")
public class HealthCheckController {

    @Value("${mavenTimestamp}")
    private String timestamp;

    @GetMapping(path = "status")
    public String status() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.of("UTC"));
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(timestamp, formatter);
        zonedDateTime = zonedDateTime.withZoneSameInstant(ZoneId.of("Asia/Singapore"));
        String timezoneTimestamp = zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd K:mm:ssa z"));

        return "OK" + " | VENDOR | " + timezoneTimestamp;
    }
}
