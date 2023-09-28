package com.nextgen.gameaggregator.entity;

import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
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
    private String vendorUsername;
    private String url;
    private String vendorBetId;
    private String roundId;
    private String vendorGameCode;
    private String gameToken;
    private String method;
    private String header;
    private String host;
    private String apiKey;
    private String signature;
    private String callerIp;
    private String userAgent;
    private String requestType;
    private String requestBody;
    private String responseBody;
    private String operatorEndPoints;
    private String operatorData;
    private Integer operatorHttpStatusCode;
    private ResponseCodes.Status operatorResponseStatus;
    private String operatorResponse;
    private String errorMessage;
    private Integer status;
    private String requestIp;
    private Long startTime;
    private Long endTime;
    private Long timeTaken;
    private Long operatorStart;
    private Long operatorEnd;
    private Long operatorTimeTaken;
    private Long betStart;
    private Long betEnd;
    private Long betTimeTaken;
}
