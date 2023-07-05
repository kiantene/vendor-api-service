package com.nextgen.gameaggregator.entity;

import lombok.Data;
import jakarta.persistence.*;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;

@Document
@Scope("raw")
@Collection("http_request_logs")
@Data
public class HttpRequestLog {
    @Id
    private String id;
    private Integer vendorId;
    private String operatorUsername;
    private String url;
    private String roundId;
    private String method;
    private String headers;
    private String requestBody;
    private String responseBody;
    private String operatorData;
    private String operatorResponse;
    private String errorMessage;
    private Integer status;
    private String requestIp;
    private Long startTime;
    private Long endTime;
    private Long timeTaken;
    private Long operatorProcessStartTime;
    private Long operatorProcessEndTime;
    private Long operatorProcessTimeTaken;
    private Long betProcessStartTime;
    private Long betProcessEndTime;
    private Long betProcessTimeTaken;
}
