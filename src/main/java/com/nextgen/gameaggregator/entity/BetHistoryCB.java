package com.nextgen.gameaggregator.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;

import javax.persistence.Id;
import java.math.BigDecimal;

@Document
@Scope("raw")
@Collection("bet_history_CB")
@Data
@AllArgsConstructor
public class BetHistoryCB {
    @Id
    private String id;
    private String externalTransactionId;
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
    private Integer resultType;
    private String rawData;
    private Integer status;
    private String gameSessionToken;
    private Long vendorBetTime;
    private Long vendorSettleTime;
    private Long createTime;
    private Long resultTime;
    private Long refundTime;
}
