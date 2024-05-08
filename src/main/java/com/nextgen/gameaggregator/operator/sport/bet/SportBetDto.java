package com.nextgen.gameaggregator.operator.sport.bet;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SportBetDto {
    private String traceId;
    private String username;
    private String transactionId;
    private String externalTransactionId;
    private String betId;
    private String roundId;
    private BigDecimal betAmount;
    private String gameCode;
    private String currency;
    private Integer betType;
    private Long timestamp;
}
