package com.nextgen.gameaggregator.vendor.lucky365.api.balance;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceResponse {

    private String code;
    private DataInfo data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DataInfo {
        private BigDecimal balance;
    }
}