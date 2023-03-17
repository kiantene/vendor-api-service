package com.nextgen.gameaggregator.controller;

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
    @Value("${mavenTimestamp}")
    private String timestamp;

    @Value("${spring.profiles.active}")
    private String profilesActive;
    @Value("${spring.datasource.maria-default.jdbc-url}")
    private String jdbcUrl;

    @Value("${spring.datasource.maria-default.username}")
    private String dbUsername;

    @Value("${spring.redis.database}")
    private String redisDB;

    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${testing.stub}")
    private String stub;

    @Value("${logstash.host}")
    private String logstashHost;

    @GetMapping(path = "status")
    public String status() {
        // log.info("Health Check OK");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.of("UTC"));
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(timestamp, formatter);
        zonedDateTime = zonedDateTime.withZoneSameInstant(ZoneId.of("Asia/Singapore"));
        String timezoneTimestamp = zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd K:mm:ssa z"));

        return "OK" + " | VENDOR | " + profilesActive + " | " + timezoneTimestamp;
    }

    @GetMapping(path = "info")
    public String info() {
        String output;

        output = "Profile:<br>" + profilesActive +
                "<br><br>" +
                "DB Info:<br>" + jdbcUrl +
                "<br>" + dbUsername +
                "<br><br>" +
                "Redis Info:<br>" + redisDB +
                "<br>" + redisHost +
                "<br><br>" +
                "Logstash Host:<br>" + logstashHost +
                "<br><br>" +
                "Testing Stub:<br>" + stub ;

        return output;
    }
}
