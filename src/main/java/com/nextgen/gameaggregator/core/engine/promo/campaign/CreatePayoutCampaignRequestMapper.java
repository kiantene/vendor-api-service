package com.nextgen.gameaggregator.core.engine.promo.campaign;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CreatePayoutCampaignRequestMapper {
    public CreatePayoutCampaignRequest toCreatePayoutCampaignRequest(String traceId, Integer campaignType, Integer vendorId, Integer vendorLineId, String vendorCampaignCode, String vendorCampaignName, String vendorTransactionId, String currencyCode, String vendorCurrencyCode, Long payoutTime) {
        return CreatePayoutCampaignRequest.builder()
                .traceId(traceId)
                .campaignType(campaignType)
                .vendorId(vendorId)
                .vendorLineId(vendorLineId)
                // The promo engine treats a blank code or name as absent anyway; null keeps it out of the payload.
                .vendorCampaignCode(blankToNull(vendorCampaignCode))
                .vendorCampaignName(blankToNull(vendorCampaignName))
                .vendorTransactionId(vendorTransactionId)
                .currencyCode(currencyCode)
                .vendorCurrencyCode(vendorCurrencyCode)
                .payoutTime(payoutTime)
                .build();
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
