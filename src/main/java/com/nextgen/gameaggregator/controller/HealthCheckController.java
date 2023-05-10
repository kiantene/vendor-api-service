package com.nextgen.gameaggregator.controller;

import lombok.extern.slf4j.Slf4j;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.couchbase.core.CouchbaseTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @Value("${spring.data.redis.database}")
    private String redisDB;

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${testing.stub}")
    private String stub;

    @Value("${spring.couchbase.connectionString}")
    private String cbConnection;

    @Value("${spring.couchbase.userName}")
    private String cbUserName;

    @Value("${spring.couchbase.password}")
    private String cbPassword;

    @Value("${spring.couchbase.bucketName}")
    private String cbBucketName;

    @Value("${spring.couchbase.scopeName}")
    private String cbScopeName;

    @Value("${spring.data.redis.mode}")
    private RedisMode redisMode;

    @Value("${spring.data.redis.nodehosts}")
    private List<String> nodeHosts;

    @GetMapping(path = "status")
    public String status() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC"));
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(timestamp, formatter);
        zonedDateTime = zonedDateTime.withZoneSameInstant(ZoneId.of("Asia/Singapore"));
        String timezoneTimestamp = zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd K:mm:ssa z"));

        return "OK" + " | VENDOR | " + profilesActive + " | " + timezoneTimestamp;
    }

    @GetMapping(path = "info")
    public String info() {
        String output;
        log.info("Health Check OK");

        output = "Profile:<br>" + profilesActive + "<br><br>" +
                "DB Info:<br>URL: " + jdbcUrl + "<br>Username: " + dbUsername + "<br><br>" +
                "Redis Info:<br>DB: " + redisDB + "<br>Host: " + redisHost + "<br>Redis NodeHost: " + nodeHosts.toString() + "<br>Mode: " + redisMode + "<br><br>" +
                "Testing Stub:<br>" + stub;

        return output;
    }

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CouchbaseTemplate couchbaseTemplate;

    @GetMapping(path = "redis")
    public String testRedisLatency() {
        long startTime = System.nanoTime();

        redisTemplate.opsForValue().get("test");

        long endTime = System.nanoTime();
        long latency = endTime - startTime;
        long milliseconds = TimeUnit.MILLISECONDS.convert(latency, TimeUnit.NANOSECONDS);
        String output = "<br>Redis Mode:<br>" + redisMode + "<br>Redis Host:<br>" + redisHost
                + "<br>Redis NodeHost:<br>" + nodeHosts.toString() + "<br><br>" + "Redis latency: " + latency
                + " nanoseconds / "
                + milliseconds + " milliseconds";

        return output;
    }

    @GetMapping(path = "db")
    public String testDbLatency() {
        long startTime = System.nanoTime();

        jdbcTemplate.queryForObject("SELECT 1", Integer.class);

        long endTime = System.nanoTime();
        long latency = endTime - startTime;
        long milliseconds = TimeUnit.MILLISECONDS.convert(latency, TimeUnit.NANOSECONDS);
        String output = "DB URL:<br>" + jdbcUrl + "<br><br>" + "Database latency: " + latency + " nanoseconds / "
                + milliseconds + " milliseconds";

        return output;
    }

    @GetMapping(path = "couchbase")
    public String testCouchbaseLatency() {
        long startTime = System.nanoTime();

        couchbaseTemplate.getCollection("result_bet");

        long endTime = System.nanoTime();
        long latency = endTime - startTime;

        long milliseconds = TimeUnit.MILLISECONDS.convert(latency, TimeUnit.NANOSECONDS);
        String output = "Couchbase latencyxxx: " + latency + " nanoseconds / " + milliseconds + " milliseconds";

        return output;
    }

    public enum RedisMode {
        CLUSTER,
        STANDALONE
    }
}
