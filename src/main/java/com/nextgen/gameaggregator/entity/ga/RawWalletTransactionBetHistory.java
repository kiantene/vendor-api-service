package com.nextgen.gameaggregator.entity.ga;

import jakarta.persistence.Id;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;

import java.math.BigDecimal;

@Document
@Scope("raw")
@TypeAlias("wallet_transaction_bet_history")
@Collection("wallet_transaction_bet_history")
@Data
public class RawWalletTransactionBetHistory {
    @Id
    private String id;
    private String token;
    private String externalTransactionId;
    private String vendorBetId;
    private String roundId;
    private BigDecimal betAmount;
    private BigDecimal winAmount;
    private BigDecimal winLoss;
    private Integer status;
    private Long createDate;
    private String vendorPlayerUsername;
}
