package com.nextgen.gameaggregator.core.engine.promo.campaign;

import com.nextgen.gameaggregator.entity.promo.Campaign;
import com.nextgen.gameaggregator.operator.constant.ResponseCodes;
import org.springframework.stereotype.Component;

@Component
public class FetchCampaignResponseMapper {
    public Campaign responseToCampaign(FetchCampaignResponse response) {

        if (response.getStatus().equals(ResponseCodes.Status.SC_OK.name())) {
            return Campaign.builder()
                    .uuid(response.getData().getUuid())
                    .status(response.getData().getStatus())
                    .campaignName(response.getData().getCampaignName())
                    .build();
        }

        return Campaign.builder().build();
    }
}
