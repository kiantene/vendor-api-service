package com.nextgen.gameaggregator.operator.sport.bet;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SportBetDto {
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
}
