package com.nextgen.gameaggregator.controller;

import lombok.extern.slf4j.Slf4j;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.couchbase.core.CouchbaseTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(path = "health/")
@Slf4j
public class HealthCheckController {

	@Value("${spring.application.name}")
	private String appName;

	@Value("${version}")
	private String appVersion;

	private final RedisTemplate<String, Object> redisTemplate;
	private final CouchbaseTemplate couchbaseTemplate;
	private final JdbcTemplate jdbcTemplate;

	public HealthCheckController(RedisTemplate<String, Object> redisTemplate, CouchbaseTemplate couchbaseTemplate,
			JdbcTemplate jdbcTemplate) {
		this.redisTemplate = redisTemplate;
		this.couchbaseTemplate = couchbaseTemplate;
		this.jdbcTemplate = jdbcTemplate;
	}

	@GetMapping(path = "status", produces = "application/json")
	public Map<String, String> status() {
		Map<String, String> info = new HashMap<>();
		info.put("name", appName);
		info.put("version", appVersion);
		return info;
	}

	@GetMapping(path = "info", produces = "application/json")
	public Map<String, Object> healthInfo() {
		Map<String, Object> info = new HashMap<>();
		info.put("redisLatency", formatLatency(measureRedisLatency()));
		info.put("couchbaseLatency", formatLatency(measureCouchbaseLatency()));
		info.put("databaseLatency", formatLatency(measureDatabaseLatency()));
		return info;
	}

	private long measureRedisLatency() {
		try {
			long start = System.nanoTime();
			redisTemplate.getConnectionFactory().getConnection().ping();
			return System.nanoTime() - start;
		} catch (Exception e) {
			log.error("Failed to measure Redis latency", e);
			return -1; // Return -1 to indicate failure
		}
	}

	private long measureCouchbaseLatency() {
		try {
			long start = System.nanoTime();
			couchbaseTemplate.getCouchbaseClientFactory().getCluster().diagnostics();
			return System.nanoTime() - start;
		} catch (Exception e) {
			log.error("Failed to measure Couchbase latency", e);
			return -1; // Return -1 to indicate failure
		}
	}

	private long measureDatabaseLatency() {
		try {
			long start = System.nanoTime();
			jdbcTemplate.queryForObject("SELECT 1", Integer.class);
			return System.nanoTime() - start;
		} catch (Exception e) {
			log.error("Failed to measure database latency", e);
			return -1; // Return -1 to indicate failure
		}
	}

	private String formatLatency(long latency) {
		if (latency < 0) {
			return "unavailable";
		} else if (latency < 1_000_000) { // less than 1 ms
			return latency + " ns";
		} else {
			return (latency / 1_000_000) + " ms";
		}
	}

	@GetMapping(path = "log-test", produces = "application/json")
	public Map<String, String> testLog(@RequestParam("level") String level, @RequestParam("message") String message) {

		// Create a HashMap
		HashMap<String, Object> hashMap = new HashMap<>();

		try {
			hashMap.put("thisIsAStringKey", "thisIsAStringValue");
			hashMap.put("thisIsAIntKey", 999);

			// Convert HashMap to JSON string
			ObjectMapper objectMapper = new ObjectMapper();
			String jsonString = objectMapper.writeValueAsString(hashMap);

			switch (level.toLowerCase()) {
			case "trace":
				log.trace(message);
				break;
			case "debug":
				log.debug(message);
				break;
			case "info":
				log.info(message);
				break;
			case "info-json":
				log.info(jsonString);
				break;
			case "warn":
				log.warn(message);
				break;
			case "error":
				// Trigger an error stack trace for demonstration
				log.error(message, simulateException());
				break;
			default:
				log.info("Unsupported log level: {}. Logging at INFO level. Message: {}", level, message);
			}
		} catch (Exception e) {
			log.error("Exception occurred while testing logs", e);
		}

		Map<String, String> response = new HashMap<>();
		response.put("logLevel", level);
		response.put("logMessage", message);
		return response;
	}

	/**
	 * Simulate an exception for logging error stack traces.
	 *
	 * @return An exception to log.
	 */
	private Exception simulateException() {
		return new RuntimeException("Simulated exception for error log");
	}
}
