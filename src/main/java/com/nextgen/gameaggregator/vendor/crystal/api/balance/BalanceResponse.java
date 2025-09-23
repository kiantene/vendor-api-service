package com.nextgen.gameaggregator.vendor.crystal.api.balance;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class BalanceResponse {

    private Data data;
    private final String error; // vendor is expecting null value

    @Getter
    @Builder
    public static class Data {
        private BigDecimal balance;
    }
}
