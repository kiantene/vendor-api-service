package com.nextgen.gameaggregator.custodianseamless.operator.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TraceIdRequest {

    private String traceId;
    private Long requestTime;
    private Integer agentId;

    public TraceIdRequest(String traceId, Integer agentId){
        this.traceId = traceId;
        this.agentId = agentId;
        this.requestTime = System.currentTimeMillis();
    }
}
