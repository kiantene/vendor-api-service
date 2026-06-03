package com.nextgen.gameaggregator.core.engine.promo.campaign;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FetchCampaignByPlayerRequest {
    private String traceId;
    private String playerUuid;
}
