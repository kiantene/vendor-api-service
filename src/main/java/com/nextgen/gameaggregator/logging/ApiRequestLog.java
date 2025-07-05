package com.nextgen.gameaggregator.logging;

import com.nextgen.gameaggregator.entity.ga.HttpRequestLog;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Data
public class ApiRequestLog {
    private static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    private String requestTime;
    private String requestType;
    private String url;
    private Integer agentId;
    private Integer vendorId;
    private String username;
    private String vendorBetId;
    private String roundId;
    private String requestBody;
    private String responseBody;
    private String operatorUrl;
    private String operatorData;
    private String operatorResponse;
    private Integer operatorHttpCode;
    private String error;
    private Integer status;
    private Long start;
    private Long end;
    private Long timeTaken;
    private Long operatorStart;
    private Long operatorEnd;
    private Long operatorTimeTaken;
    private Long betStart;
    private Long betEnd;
    private Long betTimeTaken;

    public ApiRequestLog(HttpRequestLog httpRequestLog) {
        this.requestTime = this.formatTimestamp(httpRequestLog.getStartTime());
        this.requestType = httpRequestLog.getRequestType();
        this.url = httpRequestLog.getUrl();
        this.agentId = httpRequestLog.getAgentId();
        this.vendorId = httpRequestLog.getVendorId();
        this.username = httpRequestLog.getVendorUsername();
        this.vendorBetId = httpRequestLog.getVendorBetId();
        this.roundId = httpRequestLog.getRoundId();
        this.requestBody = httpRequestLog.getRequestBody();
        this.responseBody = httpRequestLog.getResponseBody();
        this.operatorUrl = httpRequestLog.getOperatorEndPoints();
        this.operatorData = httpRequestLog.getOperatorData();
        this.operatorResponse = httpRequestLog.getOperatorResponse();
        this.operatorHttpCode = httpRequestLog.getOperatorHttpStatusCode();
        this.error = httpRequestLog.getErrorMessage();
        this.status = httpRequestLog.getStatus();
        this.start = httpRequestLog.getStartTime();
        this.end = httpRequestLog.getEndTime();
        this.timeTaken = httpRequestLog.getTimeTaken();
        this.operatorStart = httpRequestLog.getOperatorStart();
        this.operatorEnd = httpRequestLog.getOperatorEnd();
        this.operatorTimeTaken = httpRequestLog.getOperatorTimeTaken();
        this.betStart = httpRequestLog.getBetStart();
        this.betEnd = httpRequestLog.getBetEnd();
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
