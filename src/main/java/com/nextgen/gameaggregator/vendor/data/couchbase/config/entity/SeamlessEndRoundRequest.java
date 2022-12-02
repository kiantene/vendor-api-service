package com.nextgen.gameaggregator.vendor.data.couchbase.config.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;

@Document
@Scope("log")
@Collection("seamless_end_round")
@Data
@AllArgsConstructor
public class SeamlessEndRoundRequest {

    @Id
    private String id;
    private String status;
    private Long aggregatorRequestStartMs;
    private String rawRequest;
}
