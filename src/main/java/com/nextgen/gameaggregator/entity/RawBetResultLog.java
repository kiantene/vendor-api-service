package com.nextgen.gameaggregator.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;

import java.math.BigDecimal;

@Document
@Scope("raw")
@Collection("bet_result_log")
@Data
public class RawBetResultLog {
    @Id
    private String id;
    private String betHistoryId;
    private String resultLogId;
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
    private Integer status;
    private Long vendorTime;
    private Long createTime;
}
