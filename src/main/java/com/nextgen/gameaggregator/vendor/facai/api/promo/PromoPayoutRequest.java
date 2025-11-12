package com.nextgen.gameaggregator.vendor.facai.api.promo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PromoPayoutRequest {
    @JsonProperty("AgentCode")
    private String agentCode;
    @JsonProperty("Currency")
    private String currency;
    @JsonProperty("Params")
    private String params;
    @JsonProperty("Sign")
    private String sign;
    private String paramsJsonString;
}
