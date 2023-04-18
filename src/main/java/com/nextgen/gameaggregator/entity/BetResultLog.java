package com.nextgen.gameaggregator.entity;

import lombok.Data;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "bet_result_log")
@Data
public class BetResultLog {
    @Id
    private String id;
    private String betHistoryId;
    private String externalTransactionId;
    private String roundId;
    private Integer vendorGameId;
    private Long vendorPlayerId;
    private Long agentPlayerId;
    private Integer agentId;
    private Integer operatorStatus;
    private Integer vendorLineId;
    private Integer currencyId;
    private BigDecimal winAmount;
    private BigDecimal effectiveTurnover;
    private Integer resultType;
    private BigDecimal balance;
    private String rawData;
    private Integer status;
    private Long vendorTime;
    private Long createTime;
}
