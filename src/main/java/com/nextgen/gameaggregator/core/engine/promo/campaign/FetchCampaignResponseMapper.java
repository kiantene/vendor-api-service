package com.nextgen.gameaggregator.core.engine.promo.campaign;

import com.nextgen.gameaggregator.entity.promo.Campaign;
import com.nextgen.gameaggregator.promoengine.PromoEngineResponse;
import org.springframework.stereotype.Component;

@Component
public class FetchCampaignResponseMapper {
    public Campaign responseToCampaign(FetchCampaignResponse response) {

        if (PromoEngineResponse.STATUS_OK.equals(response.getStatus())) {
            return Campaign.builder()
                    .uuid(response.getData().getUuid())
                    .status(response.getData().getStatus())
                    .campaignName(response.getData().getCampaignName())
                    .build();
        }

        return Campaign.builder().build();
    }
}
