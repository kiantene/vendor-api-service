package com.nextgen.gameaggregator.core.engine.promo.campaign;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreatePayoutCampaignRequest {
    private String traceId;
    private Integer campaignType;
    private Integer vendorId;
    private Integer vendorLineId;
    private String vendorCampaignCode;
    private String vendorCampaignName;
    private String vendorTransactionId;
    private String currencyCode;
    private String vendorCurrencyCode;
    private Long payoutTime;
}
