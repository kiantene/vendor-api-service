package com.nextgen.gameaggregator.operator.sport.settle;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SportSettleDto {
    private String traceId;
    private String username;
    private String transactionId;
    private String externalTransactionId;
    private String roundId;
    private String betId;
    private String gameCode;
    private String currency;
    private Long timestamp;
    private BigDecimal betAmount;
    private BigDecimal winAmount;
    private BigDecimal winLoss;
    private BigDecimal effectiveTurnover;
}
