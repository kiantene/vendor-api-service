package com.nextgen.gameaggregator.core.engine.promo.campaign;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.promoengine.PromoEngineResponse;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class FetchCampaignResponse extends PromoEngineResponse<FetchCampaignData> {
}
