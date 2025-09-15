package com.nextgen.gameaggregator.core.retry.couchbase;

import lombok.Data;

import java.util.Map;

@Data
public class HttpRetryJob {
    private String id;

    private String method;
    private String url;
    private Map<String, String> headers;
    private String bodyJson;
}
