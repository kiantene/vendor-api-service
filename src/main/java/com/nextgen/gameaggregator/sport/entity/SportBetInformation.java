package com.nextgen.gameaggregator.sport.entity;

import java.math.BigDecimal;

public class SportBetInformation {
    private String id;
    private String betId;
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
    private String rawData;
    private Integer resettleNum;
    private Integer status;
    private String gameSessionToken;
    private Integer gameCategoryId;
    private Long vendorBetTime;
    private Long vendorSettleTime;
    private Long createTime;
    private Long resultTime;
    private Integer processingStatus;
    private BigDecimal balance;
}
