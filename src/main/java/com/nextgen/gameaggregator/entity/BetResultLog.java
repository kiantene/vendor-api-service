package com.nextgen.gameaggregator.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "bet_result_log")
@Data
public class BetResultLog {
    @Id
    private String id;
    private String referenceTransactionId;
    private String externalTransactionId;
    private String roundId;
    private Integer vendorGameId;
    private Long vendorPlayerId;
    private Long agentPlayerId;
    private Integer agentId;
    private Integer currencyId;
    private BigDecimal winAmount;
    private Integer resultType;
    private BigDecimal balance;
    private String rawData;
    private Integer status;
    private Long vendorTime;
    private Long createDate;
}
