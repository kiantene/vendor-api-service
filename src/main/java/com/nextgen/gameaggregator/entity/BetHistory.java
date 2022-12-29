package com.nextgen.gameaggregator.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "bet_history")
@Data
public class BetHistory {
    @Id
    private String id;
    private String externalTransactionId;
    private String roundId;
    private Integer vendorGameId;
    private Long vendorPlayerId;
    private Integer vendorId;
    private Long agentPlayerId;
    private Integer agentId;
    private Integer masterAgentId;
    private Integer houseId;
    private Integer gameCategoryId;
    private Integer currencyId;
    private BigDecimal betAmount;
    private BigDecimal winAmount;
    private BigDecimal winLoss;
    private BigDecimal vendorWinLoss;
    private BigDecimal effectiveTurnover;
    private Integer resultType;
    private Integer status;
    private Long vendorBetTime;
    private Long vendorSettleTime;
    private Long createDate;
}
