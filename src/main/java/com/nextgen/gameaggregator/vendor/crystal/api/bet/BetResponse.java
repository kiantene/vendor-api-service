package com.nextgen.gameaggregator.vendor.crystal.api.bet;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@Builder
public class BetResponse {

    private Data data;
    private final String error; // vendor is expecting null value

    @Getter
    @Builder
    public static class Data {
        private BigDecimal balance;
        private String actionId;
    }
}
