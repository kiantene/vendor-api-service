package com.nextgen.gameaggregator.vendor.crystal.api.rollback;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class RollbackResponse {
    private Data data;
    private final String error; // vendor is expecting null value

    @Getter
    @Setter
    @Builder
    public static class Data {
        private BigDecimal balance;
        private String actionId;
    }

}