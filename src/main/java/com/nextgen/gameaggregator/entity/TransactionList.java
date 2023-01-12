package com.nextgen.gameaggregator.entity;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "bet_history")
public class TransactionList {

    @Id
    private String transactionId;
    private String externalRoundId;
    private String externalTransactionId;
    private Integer gameId;
    private BigDecimal betAmount;
    private BigDecimal winAmount;
    private BigDecimal winLoss;
    private BigDecimal effectiveTurnover;
    private Integer resultType;
    private Integer status;
    private Long vendorBetTime;
    private Long vendorSettleTime;
    private Long createTime;
    private String username;
    private String categoryCode;
    private String vendorCode;
    private String currency;
    private String gameCode;
}
