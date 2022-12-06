package com.nextgen.gameaggregator.vendor.data.couchbase.config.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;

@Document
@Scope("log")
@Collection("seamless_refund_log")
@Data
@AllArgsConstructor
public class SeamlessRefundLogRequest {

    @Id
    private String vendorBetId;
    private String vendorCode;
    private String status;
    private Long aggregatorRequestStartMs;
    private String rawRequest;
}
