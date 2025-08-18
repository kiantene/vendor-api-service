package com.nextgen.gameaggregator.vendor.crystal.api.settle;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
public class SettleResponse {
    private Data data;
    private Error error;

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static class Data {
        private BigDecimal balance;
        private String actionId;
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static class Error {
    }
}