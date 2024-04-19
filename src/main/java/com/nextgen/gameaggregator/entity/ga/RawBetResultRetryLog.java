package com.nextgen.gameaggregator.entity.ga;

import jakarta.persistence.Id;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;

@Document
@Scope("raw")
@TypeAlias("bet_result_retry_log")
@Collection("bet_result_retry_log")
@Data
public class RawBetResultRetryLog {
    @Id
    private String id;
    private String transactionId;
    private String action;
    private Integer vendorId;
    private Integer agentId;
    private String operatorData;
    private Integer retryCounter;
    private Long nextRetryTime;
    private Integer status;
    private Long createDate;
}
