package com.nextgen.gameaggregator.vendor.crystal.api.balance;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class BalanceResponse {

    private Data data;
    private Error error;

    @Getter
    @Builder
    public static class Data {
        private BigDecimal balance;
    }

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public static class Error {
    }
}
