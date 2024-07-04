package com.nextgen.gameaggregator.logging;

import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Data
public class ApiResponseLog {
    private static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    private String id;
    private Integer agentId;
    private Integer vendorId;
    private String url;
    private String operatorEndPoints;
    private String requestTime;
    private String requestEnd;
    private Long requestTimeTaken;
    private Long operatorTimeTaken;
    private Long betTimeTaken;

    public ApiResponseLog(HttpRequestLog httpRequestLog) {
        this.id = httpRequestLog.getId();
        this.agentId = httpRequestLog.getAgentId();
        this.vendorId = httpRequestLog.getVendorId();
        this.url = httpRequestLog.getUrl();
        this.operatorEndPoints = httpRequestLog.getOperatorEndPoints();
        this.requestTime = this.formatTimestamp(httpRequestLog.getStartTime());
        this.requestEnd = this.formatTimestamp(httpRequestLog.getEndTime());
        this.requestTimeTaken = httpRequestLog.getTimeTaken();
        this.operatorTimeTaken = httpRequestLog.getOperatorTimeTaken();
        this.betTimeTaken = httpRequestLog.getBetTimeTaken();
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

