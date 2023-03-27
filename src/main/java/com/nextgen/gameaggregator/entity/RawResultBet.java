package com.nextgen.gameaggregator.entity;

import lombok.Data;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;

import javax.persistence.Id;
import java.math.BigDecimal;

@Document
@Scope("raw")
@Collection("result_bet")
@Data
public class RawResultBet {
    @Id
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
    private String md5RawSettledResult;
    private Integer resettleNum;
    private Integer status;
    private String gameSessionToken;
    private Long vendorBetTime;
    private Long vendorSettleTime;
    private Long createTime;
    private Long resultTime;
    private Long refundTime;
}
