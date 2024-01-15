package com.nextgen.gameaggregator.operator.sport.resettle;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SportResettleDto {
    private String traceId;
    private String username;
    private String transactionId;
    private String externalTransactionId;
    private String roundId;
    private String betId;
    private String gameCode;
    private String currency;
    private BigDecimal betAmount;
    private BigDecimal winAmount;
    private BigDecimal winLoss;
    private BigDecimal effectiveTurnover;
    private BigDecimal newWinAmount;
    private BigDecimal creditAmount;
    private BigDecimal debitAmount;
    private Long timestamp;

}
