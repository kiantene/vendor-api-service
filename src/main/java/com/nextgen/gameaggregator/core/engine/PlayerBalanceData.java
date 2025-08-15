package com.nextgen.gameaggregator.core.engine;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class PlayerBalanceData {
    private String username;
    private BigDecimal balance;
    private String currency;
    private Long timestamp;
}
