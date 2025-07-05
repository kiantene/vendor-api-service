package com.nextgen.gameaggregator.core.logging;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogContext {

    private static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";
    private final Map<String, Object> extraFields = new LinkedHashMap<>(); // LinkedHashMap to maintain field ordering
    private String time;
    private String type;
    private String url;
    private String traceId;
    private String logGroup;
    private String body;
    private String response;
    private long start;
    private long end;
    private long timeTaken;
    private String exception;
    private String errorMessage;
    private int status;
    private String vendorClassName;

    public LogContext() {
        this.logGroup = "general";
        this.traceId = UUID.randomUUID().toString();
        this.start = System.currentTimeMillis();
        this.time = this.formatTimestamp(this.start);
    }

    public void setEnd() {
        this.end = System.currentTimeMillis();
        this.timeTaken = this.end - this.start;
    }

    public void put(String key, Object value) {
        extraFields.put(key, value);
    }

    private String formatTimestamp(Long timestamp) {
        // Convert timestamp to Instant
        Instant instant = Instant.ofEpochMilli(timestamp);
        // Convert Instant to LocalDateTime
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        // Define the formatter
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATETIME_FORMAT);

        return dateTime.format(formatter) + "Z";
    }

    public String toJson() {
        try {
            Map<String, Object> base = new LinkedHashMap<>();
            base.put("time", time);
            base.put("logGroup", logGroup);
            base.put("traceId", traceId);
            base.put("type", type);
            base.put("url", url);
            base.put("start", start);
            base.put("end", end);
            base.put("timeTaken", timeTaken);
            base.put("body", body);
            base.put("status", status);
            base.put("response", response);

            // Exception
            base.put("exception", exception);
            base.put("errorMessage", errorMessage);
            base.putAll(extraFields);

            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

            return mapper.writeValueAsString(base);

        } catch (JsonProcessingException jsonProcessingException) {
            return this.toString();
        }
    }
}
