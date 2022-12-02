package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_180.bet;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BetActionVo {
    private String transactionId;
    private String currency;
    private BigDecimal cash;
    private BigDecimal bonus;
    private BigDecimal usedPromo;
    private Integer error;
    private String description;
}
