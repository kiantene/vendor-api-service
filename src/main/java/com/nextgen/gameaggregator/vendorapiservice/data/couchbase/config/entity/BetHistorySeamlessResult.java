package com.nextgen.gameaggregator.vendorapiservice.data.couchbase.config.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;

@Document
@Scope("raw")
@Collection("bet_history_seamless_result")
@Data
@AllArgsConstructor
public class BetHistorySeamlessResult {

    @Id
    private String betHistoryId;
    private String type;
    private String categoryCode;
    private String vendorCode;
    private String vendorCurrencyCode;
    private String rawResponse;
    private String aggregatorRequestStartMs;
}
