package com.nextgen.gameaggregator.operator.sport.resettle;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SportResettleDto {
    private String traceId;
    private String username;
    private String transactionId;
    private String externalTransactionId;
    private String betId;
    private String roundId;
    private BigDecimal betAmount;
    private BigDecimal winAmount;
    private BigDecimal newWinAmount;
    private BigDecimal effectiveTurnover;
    private BigDecimal winLoss;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private String gameCode;
    private String currency;
    private Long timestamp;

}
