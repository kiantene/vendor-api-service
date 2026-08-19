package com.nextgen.gameaggregator.vendor.evoplay.api.freeround;

import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContextMapper;
import com.nextgen.gameaggregator.enums.PromoType;
import org.springframework.stereotype.Component;

@Component
public class EvoplayFreeRoundPayoutRequestMapper implements PromoPayoutContextMapper<EvoplayFreeRoundPayoutRequest> {

    @Override
    public PromoPayoutContext toInternal(EvoplayFreeRoundPayoutRequest request) {
        return PromoPayoutContext.builder()
                .idempotencyKey(request.getId())
                .vendorTransactionId(request.getId())
                .vendorPlayerUsername(request.getUserId())
                .vendorCurrency(request.getCurrency())
                .vendorPayoutAmount(request.getAmount())
                .vendorCampaignCode(request.getEventId())
                .promoType(PromoType.FREE_ROUND)
                .build();
    }
}
