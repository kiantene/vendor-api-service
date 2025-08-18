package com.nextgen.gameaggregator.vendor.crystal.api.bet;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class BetResponse {

    private Data data;
    private Error error;

    @Getter
    @Builder
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
