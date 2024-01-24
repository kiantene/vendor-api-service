package com.nextgen.gameaggregator.entity.ga;

import jakarta.persistence.Id;
import lombok.Data;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;

import java.math.BigDecimal;

@Document
@Scope("raw")
@Collection("bet_adjustment_log")
@Data
public class RawBetAdjustmentLog {
    @Id
    private String id; // couchbase primary key
    private String betAdjustmentId; // mariadb primary key
    private String betHistoryId;
    private String externalTransactionId;
    private String roundId;
    private BigDecimal amount;
    private Integer vendorLineId;
    private Integer vendorGameId;
    private Long vendorPlayerId;
    private Long agentPlayerId;
    private Integer agentId;
    private Integer operatorStatus;
    private BigDecimal balance;
    private Integer currencyId;
    private Long createTime;
}