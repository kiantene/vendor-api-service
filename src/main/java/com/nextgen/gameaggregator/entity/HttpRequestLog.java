package com.nextgen.gameaggregator.entity;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "http_request_logs")
@Data
public class HttpRequestLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String traceId;
    private String url;
    private String method;
    private String headers;
    private String requestBody;
    private String responseBody;
    private String stackTrace;
    private Integer status;
    private String requestIp;
    private Long requestTime;
}
