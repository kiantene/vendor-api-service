package com.nextgen.gameaggregator.vendor.pragmaticplayv2.api.promo.freeround;

import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContextMapper;
import com.nextgen.gameaggregator.enums.PromoType;
import org.springframework.stereotype.Component;

@Component
public class FreeRoundPayoutRequestMapper implements PromoPayoutContextMapper<FreeRoundPayoutRequest> {
    @Override
    public PromoPayoutContext toInternal(FreeRoundPayoutRequest vendorRequest) {
        return PromoPayoutContext.builder()
                .idempotencyKey(vendorRequest.getReference())
                .vendorPlayerUsername(vendorRequest.getUserId())
                // promo payout history
                .vendorCampaignCode(vendorRequest.getBonusCode())
                .vendorTransactionId(vendorRequest.getReference())
                .vendorPayoutAmount(vendorRequest.getAmount())
                .vendorTransactionTime(vendorRequest.getTimestamp())
                .promoType(PromoType.FREE_ROUND)
                .build();
    }
}
