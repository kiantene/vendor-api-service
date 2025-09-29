package com.nextgen.gameaggregator.core.retry;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class HttpCallSpec {
    private String traceId;
    private String idempotencyKey;
    private int partition;
    private String partitionKey;
    private Integer agentId;
    private String method;
    private String url;
    private Map<String, String> headers;
    private String bodyJson;
    private long requestTime;
    private Long transactionTime;
}
