package com.nextgen.gameaggregator.entity.ga;

import jakarta.persistence.Id;
import lombok.Data;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;

import java.math.BigDecimal;

@Document
@Scope("raw")
@Collection("bet_refund_logs")
@Data
public class RawBetRefundLog {
    @Id
    private String id; // couchbase primary key
    private String betRefundLogId; // mariadb primary key
    private String betHistoryId;
    private String externalTransactionId;
    private String roundId;
    private Integer vendorGameId;
    private Long vendorPlayerId;
    private Integer vendorLineId;
    private Long agentPlayerId;
    private Integer agentId;
    private Integer operatorStatus;
    private Integer currencyId;
    private Long createTime;
    private BigDecimal balance;
}
