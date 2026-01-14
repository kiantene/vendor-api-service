package com.nextgen.gameaggregator.vendor.lucky365.api.bet;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BetResponse {

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