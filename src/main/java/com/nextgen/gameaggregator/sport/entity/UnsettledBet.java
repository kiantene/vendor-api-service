package com.nextgen.gameaggregator.sport.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;

import java.math.BigDecimal;

@Document
@Scope("sport")
@Collection("unsettled_bet")
@Data
@NoArgsConstructor
public class UnsettledBet {
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
    private BigDecimal actualBetAmount;
    private BigDecimal effectiveTurnover;
    private BigDecimal odds;
    private Integer oddTypeId;
}
