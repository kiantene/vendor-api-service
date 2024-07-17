package com.nextgen.gameaggregator.entity.ga;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nextgen.gameaggregator.core.WalletRequest;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import jakarta.persistence.Id;
import lombok.Data;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

import java.util.Optional;
import java.util.UUID;

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
    private Long operatorTimestamp;
    private boolean responseLogged;

    @JsonIgnore
    private WalletRequest walletRequest;

    public HttpRequestLog() {
        this.id = UUID.randomUUID().toString();
        this.startTime = System.currentTimeMillis();
        walletRequest = new WalletRequest(this.id);
        this.responseLogged = true;
    }

    public HttpRequestLog(HttpRequestLog httpRequestLog) {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        modelMapper.getConfiguration().setSkipNullEnabled(true);
        modelMapper.map(httpRequestLog, this);
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
        this.timeTaken = this.endTime - this.startTime;
    }

    public void setOperatorEnd(Long operatorEnd) {
        this.operatorEnd = operatorEnd;

        if (operatorEnd != null && this.operatorStart != null) {
            this.operatorTimeTaken = operatorEnd - this.operatorStart;
        }
    }

    public void setBetEnd(Long betEnd) {
        this.betEnd = betEnd;

        if (betEnd != null && this.betStart != null) {
            long operatorTime = Optional.ofNullable(this.operatorTimeTaken).orElse(0L);
            this.betTimeTaken = betEnd - this.betStart - operatorTime;
        }
    }
}
