package com.nextgen.gameaggregator.vendor.api.pragmaticplay.v1_180.bonuswin;

import com.couchbase.client.core.deps.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.vendor.api.pragmaticplay.component.dto.AbstractActionDto;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BonusWinActionDto extends AbstractActionDto {
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
