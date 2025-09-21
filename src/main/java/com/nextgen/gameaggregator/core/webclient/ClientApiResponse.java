package com.nextgen.gameaggregator.core.webclient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.core.engine.PlayerBalanceData;
import lombok.Data;

@Data
public class ClientApiResponse {
    private String traceId;
    private String status;
    private String message;

    @JsonIgnoreProperties(ignoreUnknown = true)
    private PlayerBalanceData data;
}
