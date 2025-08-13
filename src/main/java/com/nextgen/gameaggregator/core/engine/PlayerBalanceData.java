package com.nextgen.gameaggregator.core.engine;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;

@Builder
@Data
@Jacksonized
public class PlayerBalanceData {
    private String username;
    private BigDecimal balance;
    private String currency;
    private Long timestamp;
}
