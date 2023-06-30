package com.nextgen.gameaggregator.operator.apiverification.agentinfo;

import com.nextgen.gameaggregator.service.HttpResponse;
import lombok.Data;

@Data
public class AgentInfoVo<T> implements HttpResponse {

    private String traceId;
    private String currency;
    private String gameCode;
    private String username;
    private String apiKey;
    private String apiSecret;
    private String error;

    @Override
    public boolean hasError() {
        return false;
    }
}
