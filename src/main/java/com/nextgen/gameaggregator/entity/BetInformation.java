package com.nextgen.gameaggregator.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public abstract class BetInformation {
    private String id;
    private String internalTransactionId;
    private String externalTransactionId;
    private String vendorBetId;
    private String roundId;
    private Integer vendorGameId;
    private Long vendorPlayerId;
    private Integer vendorId;
    private Integer vendorLineId;
    private Long agentPlayerId;
    private Integer agentId;
    private Integer operatorStatus;
    private Integer currencyId;
    private BigDecimal betAmount;
    private BigDecimal winAmount;
    private BigDecimal jackpotAmount;
    private BigDecimal winLoss;
    private BigDecimal effectiveTurnover;
    private Integer resultType;
    private Integer isFreespin;
    private Integer status;
    private String gameSessionToken;
    private Long vendorBetTime;
    private Long vendorSettleTime;
    private Long createTime;
    private Long resultTime;
}
