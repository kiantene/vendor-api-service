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
    private Integer agentId;
    private Integer vendorId;
    private String operatorUsername;
    private String url;
    private String vendorBetId;
    private String roundId;
    private String gameToken;
    private String method;
    private Object headers;
    private String requestType;
    private String requestBody;
    private Object requestBodyDto;
    private Object responseBody;
    private Object operatorData;
    private Integer operatorResponseCode;
    private Object operatorResponse;
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
