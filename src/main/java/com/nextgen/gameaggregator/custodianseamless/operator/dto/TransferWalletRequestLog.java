package com.nextgen.gameaggregator.custodianseamless.operator.dto;

import jakarta.persistence.Id;
import lombok.Data;

@Data
public class TransferWalletRequestLog {
    @Id
    private String url;
    private String method;
    private String header;
    private String host;
    private String callerIp;
    private String userAgent;
    private String requestType;
    private String requestIp;

    private String apiKey;
    private String signature;
    private String requestBody;
    private String responseBody;
    private Integer agentId;
    private String operatorUsername;

    private Long walletServiceStart;
    private Long walletServiceEnd;
    private Long walletServiceTimeTaken;
    private String walletServiceEndPoints;
    private String walletServiceData;
    private String walletServiceResponse;
    private Integer walletServiceHttpStatusCode;

    private Integer status;

    private Long startTime;
    private Long endTime;
    private Long timeTaken;
}
