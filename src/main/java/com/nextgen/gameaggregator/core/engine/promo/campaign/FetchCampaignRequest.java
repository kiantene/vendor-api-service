package com.nextgen.gameaggregator.core.engine.promo.campaign;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FetchCampaignRequest {
    private String traceId;
    private Integer vendorLineId;
    private String vendorLineUuid;
    private String vendorCampaignCode;
    private Integer campaignType;
}
