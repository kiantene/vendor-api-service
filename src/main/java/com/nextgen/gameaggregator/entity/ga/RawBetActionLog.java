package com.nextgen.gameaggregator.entity.ga;

import com.nextgen.gameaggregator.operator.enums.ResultType;
import jakarta.persistence.Id;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;

@Document
@Scope("raw")
@TypeAlias("bet_action_log")
@Collection("bet_action_log")
@Data
public class RawBetActionLog {
    @Id
    private String id;
    private String token;
    private String processData;
    private String externalTransactionId;
    private String vendorBetId;
    private String roundId;
    private Integer action;
    private Integer status;
    private Long createDate;
    private String vendorPlayerUsername;
    private Integer retryCounter;
    private Boolean settleByBet;
    private ResultType resultType;
    private Long nextRetryTime;
}
