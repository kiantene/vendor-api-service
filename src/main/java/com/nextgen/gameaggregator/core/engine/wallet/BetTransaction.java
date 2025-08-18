package com.nextgen.gameaggregator.core.engine.wallet;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BetTransaction {
    private String vendorBetId;
    private String roundId;
    private String betType;
    private BigDecimal betAmount;
    private BigDecimal winAmount;
    private BigDecimal winLoss;
    private BigDecimal effectiveTurnover;
    private Long vendorBetTime;
    private Long vendorSettleTime;
}
