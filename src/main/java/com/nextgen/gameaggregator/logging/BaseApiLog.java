package com.nextgen.gameaggregator.logging;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Data
public abstract class BaseApiLog {
    private static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    private String ver;
    private String server;
    private String requestTime;
    protected String traceId;
    protected String requestType;
    protected String requestBody;
    protected String responseBody;
    protected Integer status;
    protected String exception;
    protected String exceptionMessage;

    protected Long start;
    protected Long end;
    protected Long timeTaken;
    protected Long gaTimeTaken;

    protected BaseApiLog() {
        this.traceId = UUID.randomUUID().toString();
        this.start = System.currentTimeMillis();
        this.requestTime = this.formatTimestamp(this.start);
    }

    public void setEnd(Long end) {
        this.end = end;
        this.timeTaken = this.end - this.start;
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
}
