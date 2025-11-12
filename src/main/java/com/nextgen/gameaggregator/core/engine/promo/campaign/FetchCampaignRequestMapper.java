package com.nextgen.gameaggregator.core.engine.promo.campaign;

import com.nextgen.core.util.UuidUtil;
import org.springframework.stereotype.Component;

@Component
public class FetchCampaignRequestMapper {
    public FetchCampaignRequest toFetchCampaignRequest(String traceId, String vendorCampaignCode, Integer vendorLineId, Integer promoType) {

        return FetchCampaignRequest.builder()
                .traceId(traceId)
                .vendorCampaignCode(vendorCampaignCode)
                .vendorLineId(vendorLineId)
                .vendorLineUuid(UuidUtil.newUuidV7StringRaw()) // TODO : to enhance
                .campaignType(promoType)
                .build();
    }
}
