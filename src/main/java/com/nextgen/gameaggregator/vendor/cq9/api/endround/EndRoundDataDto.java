package com.nextgen.gameaggregator.vendor.cq9.api.bet;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BetDataDto {
    private String mtcode;
    private BigDecimal amount;
    private String eventtime;
}
