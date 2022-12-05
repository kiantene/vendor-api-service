package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_180.bonuswin;

import com.couchbase.client.core.deps.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BonusWinActionDto {
    private String hash;
    private String userId;
    private String amount;
    private String reference;
    private String providerId;
    private String timestamp;
    private String bonusCode;
    private String roundId;
    private String gameId;
    private String token;
}
