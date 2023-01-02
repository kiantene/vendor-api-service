package com.nextgen.gameaggregator.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "bet_refund_log")
@Data
public class BetRefundLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;
    private String betHistoryId;
    private String externalTransactionId;
    private String roundId;
    private Integer vendorGameId;
    private Long vendorPlayerId;
    private Long agentPlayerId;
    private Integer agentId;
    private Integer currencyId;
    private BigDecimal balance;
    private String rawData;
    private Integer status;
    private Long createTime;
}
