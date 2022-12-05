package com.nextgen.gameaggregator.vendor.data.couchbase.config.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.repository.Collection;
import org.springframework.data.couchbase.repository.Scope;

import javax.persistence.Id;
import java.util.UUID;

@Data
@Document
@Scope("log")
@Collection("vendor_player_authentication")
//@AllArgsConstructor
public class VendorPlayerAuthentication {

    @Id
    private String id;

    private Long vendorId;

    private Long walletType;

    private Long agentPlayerId;

    private Long vendorPlayerId;

    private String vendorPlayerUsername;

    private String platformCode;

    private String vendorPlatformCode;

    private String languageCode;

    private String vendorLanguageCode;

    private Long gameId;

    private String vendorGameCode;

    private Long agentId;

    private String traceId;

    private String currencyCode;

    private String vendorCurrencyCode;

    private Boolean status;

    private Long createdAt;

}

