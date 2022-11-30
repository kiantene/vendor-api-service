package com.nextgen.gameaggregator.vendorapiservice.data.couchbase.config.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;


@Document
@Scope("log")
@Collection("seamless_bet_history_request")
@Data
@AllArgsConstructor
public class SeamlessBetHistoryRequest {

    @Id
    private String serviceVendorBetId;

    private String vendorBetId;

    private String betHistoryId;

    private String vendorRoundId;

    private Double betAmount;

    private Long betTime;

    private Long receivedTime;

    private String requestType;

    private String gameCategory;

    private String rawResponse;

    private String vendorCode;

}
