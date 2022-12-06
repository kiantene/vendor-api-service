package com.nextgen.gameaggregator.vendor.data.couchbase.config.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;

@Document
@Scope("log")
@Collection("seamless_end_round_error")
@Data
@AllArgsConstructor
public class SeamlessEndRoundErrorRequest {

    private String id;
    private Long aggregatorRequestStartMs;
    private String rawRequest;
}
