package com.nextgen.gameaggregator.entity.ga;

import jakarta.persistence.Id;
import lombok.Data;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;

import java.math.BigDecimal;

@Document
@Scope("raw")
@Collection("player_balance")
@Data
public class PlayerBalance {
    @Id
    private String id;
    private Integer agentId;
    private Long agentPlayerId;
    private String agentPlayerUsername;
    private String vendorPlayerUsername;
    private Long vendorPlayerId;
    private String currencyCode;
    private BigDecimal balance;
    private Long createTime;
}
