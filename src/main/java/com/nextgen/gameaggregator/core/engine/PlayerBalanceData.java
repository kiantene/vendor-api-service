package com.nextgen.gameaggregator.core.engine;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PlayerBalanceData {
    private String username;
    private BigDecimal balance;
    private String currency;
    private Long timestamp;
}
