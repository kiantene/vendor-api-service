package com.nextgen.gameaggregator.entity.ga;

import com.nextgen.gameaggregator.operator.wallet.bet.BetData;
import jakarta.persistence.Id;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;

import java.math.BigDecimal;

@Document
@Scope("raw")
@TypeAlias("bet_result_logs")
@Collection("bet_result_logs")
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
    private String vendorCurrencyCode;
    private BigDecimal winAmount;
    private BigDecimal effectiveTurnover;
    private Integer resultType;
    private BigDecimal balance;
    private Integer status;
    private Long vendorTime;
    private Long createTime;
}
