package com.nextgen.gameaggregator.custodianseamless.operator.dto;

import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import jakarta.persistence.Id;
import lombok.Data;
import org.springframework.util.MultiValueMap;

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
    private ResponseCodes.Status responseStatus;
    private Integer agentId;
    private String operatorUsername;
    private String currencyCode;

    private Long walletServiceStart;
    private Long walletServiceEnd;
    private Long walletServiceTimeTaken;
    private String walletServiceEndPoints;
    private MultiValueMap<String, String> walletServiceHeader;
    private Object walletServiceData;
    private String walletServiceResponse;
    private ResponseCodes.Status walletServiceResponseStatus;
    private Integer walletServiceHttpStatusCode;

    private String errorException;
    private String errorExceptionMessage;
    private Integer status;

    private Long startTime;
    private Long endTime;
    private Long gaTimeTaken;
    private Long timeTaken;
}
