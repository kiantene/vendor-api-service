package com.nextgen.gameaggregator.core.engine.promo.campaign;

import com.nextgen.gameaggregator.entity.promo.Campaign;
import com.nextgen.gameaggregator.promoengine.PromoEngineResponse;
import org.springframework.stereotype.Component;

@Component
public class CreatePayoutCampaignResponseMapper {
    /**
     * Throws rather than returning an empty campaign, as the fetch mapper does: the result is cached by
     * vendor campaign code, and an empty one would label every payout of that campaign with nothing until
     * the entry expired.
     */
    public Campaign responseToCampaign(CreatePayoutCampaignResponse response) {
        if (!PromoEngineResponse.STATUS_OK.equals(response.getStatus()) || response.getData() == null) {
            throw new IllegalStateException("createPayoutCampaign failed: status=" + response.getStatus()
                    + " message=" + response.getMessage());
        }

        return Campaign.builder()
                .uuid(response.getData().getUuid())
                .status(response.getData().getStatus())
                .campaignName(response.getData().getCampaignName())
                .build();
    }
}
