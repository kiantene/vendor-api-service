package com.nextgen.gameaggregator.scheduler.betaction;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GeneralDebitRollbackDto {

    private String vendorPlayerUsername;
    private String externalTransactionId;
    private String roundId;
    private String vendorGameCode;
    private Long timestamp;
    private String token;
    private String vendorBetId;
    private Integer takeAll;
    private BigDecimal transferAmount;
    private BigDecimal betAmount;
    private BigDecimal winAmount;
    private BigDecimal effectiveTurnover;
    private BigDecimal jackpotAmount;
    private Integer resultType;
    private Long vendorBetTime;
    private Long vendorSettleTime;

}