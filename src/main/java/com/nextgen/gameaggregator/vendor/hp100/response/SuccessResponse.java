package com.nextgen.gameaggregator.vendor.hp100.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SuccessResponse {
    private String userId;
    private String currency;
    private String balance;
    private String userName;
    private String txId;
    private String sessionId;
}
