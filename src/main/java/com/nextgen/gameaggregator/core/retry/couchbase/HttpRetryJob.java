package com.nextgen.gameaggregator.core.retry.couchbase;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Data
public class HttpRetryJob {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withZone(ZoneOffset.UTC);

    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("id")
    private String id;
    @JsonProperty("traceId")
    private String traceId;
    @JsonProperty("origin")
    private String origin;
    @JsonProperty("partition")
    private int partition;

    // ------ Request Info ------
    @JsonProperty("method")
    private String method;
    @JsonProperty("url")
    private String url;
    @JsonProperty("headers")
    private Map<String, String> headers;
    @JsonProperty("bodyJson")
    private String bodyJson;

    // ------ Scheduling Info ------
    @JsonProperty("attempts")
    private int attempts;
    @JsonProperty("nextRunAt")
    private long nextRunAt;

    @JsonProperty("createdTs")
    private long createdTs;

    public HttpRetryJob() {
        this.createdTs = System.currentTimeMillis();
        this.createdAt = DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(this.createdTs));
        this.attempts = 0;
        this.nextRunAt = System.currentTimeMillis();
    }
}
