package com.nextgen.gameaggregator.operator.sport.settle;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SportSettleDto {
    private String traceId;
    private String username;
    private String transactionId;
    private String externalTransactionId;
    private String betId;
    private String roundId;
    private BigDecimal betAmount;
    private BigDecimal winAmount;
    private BigDecimal effectiveTurnover;
    private BigDecimal winLoss;
    private String gameCode;
    private String currency;
    private Long timestamp;
}
