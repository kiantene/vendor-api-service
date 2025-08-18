package com.nextgen.gameaggregator.core.engine.wallet;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BetTransaction {
    /**
     * Unique identifier for this individual transaction as provided by the vendor.
     * This ID uniquely identifies each transaction record.
     */
    private String externalTransactionId;
    /**
     * Vendor-provided bet identifier that groups related transactions together.
     * Multiple transactions may share the same vendorBetId when they represent
     * different outcomes of a single betting event.
     * Can be the same value as externalTransactionId.
     */
    private String vendorBetId;
    private String roundId;
    /**
     * Types of bet (normal, parley, side-bets eg. 21+3, Perfect Pair)
     */
    private String betType;
    private BigDecimal betAmount;
    private BigDecimal winAmount;
    private BigDecimal winLoss;
    private BigDecimal effectiveTurnover;
    private Long vendorBetTime;
    private Long vendorSettleTime;
}
