package com.nextgen.gameaggregator.core.engine.promo.player;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nextgen.gameaggregator.promoengine.PromoEngineResponse;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class FindActivePlayerCampaignResponse extends PromoEngineResponse<FindActivePlayerCampaignResponse.Data> {

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {
        private String vendorCampaignCode;
    }
}
