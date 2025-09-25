package com.nextgen.gameaggregator.core.retry;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class HttpCallSpec {
    private String traceId;
    private String idempotencyKey;
    private String method;
    private String url;
    private Map<String, String> headers;
    private String bodyJson;
    private int partition;
    private long requestTime;
}
