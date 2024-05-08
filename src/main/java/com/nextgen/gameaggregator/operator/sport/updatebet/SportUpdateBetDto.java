package com.nextgen.gameaggregator.operator.sport.updatebet;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SportUpdateBetDto {
    private String traceId;
    private String username;
    private String transactionId;
    private String externalTransactionId;
    private String betId;
    private String roundId;
    private BigDecimal betAmount;
    private BigDecimal newBetAmount;
    private BigDecimal creditAmount;
    private String gameCode;
    private String currency;
    private Long timestamp;
}
