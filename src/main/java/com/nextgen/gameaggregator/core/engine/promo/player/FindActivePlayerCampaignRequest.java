package com.nextgen.gameaggregator.core.engine.promo.player;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FindActivePlayerCampaignRequest {
    private String traceId;
    private String currencyCode;
    private Integer vendorLineId;
    private String vendorPlayerUsername;
    private String vendorGameCode;
}
