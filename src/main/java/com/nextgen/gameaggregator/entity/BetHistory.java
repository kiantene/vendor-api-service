package com.nextgen.gameaggregator.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "bet_history")
@Data
public class BetHistory {
    @Id
    private String id;
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
    private Integer masterAgentId;
    private Integer houseId;
    private Integer gameCategoryId;
    private Integer currencyId;
    private BigDecimal betAmount;
    private BigDecimal winAmount;
    private BigDecimal winLoss;
    private BigDecimal vendorWinLoss;
    private BigDecimal effectiveTurnover;
    private BigDecimal refundAmount;
    private BigDecimal jackpotAmount;
    private Integer resultType;
    private Integer isCancelled;
    private Integer isFreespin;
    private String rawData;
    private Integer resettleNum;
    private Integer status;
    private String gameSessionToken;
    private Long vendorBetTime;
    private Long vendorSettleTime;
    private Long createTime;
    private Long resultTime;
    private Long refundTime;
}
