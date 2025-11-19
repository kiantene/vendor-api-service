package com.nextgen.gameaggregator.vendor.jdb.api.promo;

import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContext;
import com.nextgen.gameaggregator.core.engine.promo.payout.PromoPayoutContextMapper;
import com.nextgen.gameaggregator.enums.PromoType;
import org.springframework.stereotype.Component;

@Component
public class PromoPayoutRequestMapper implements PromoPayoutContextMapper<PromoPayoutRequest> {
    @Override
    public PromoPayoutContext toInternal(PromoPayoutRequest vendorRequest) {
        return PromoPayoutContext.builder()
                .idempotencyKey(vendorRequest.getTransferId().toString())
                .vendorPlayerUsername(vendorRequest.getUid())
                .vendorCurrency(vendorRequest.getCurrency())
                .vendorCampaignCode(vendorRequest.getEventId())
                .vendorTransactionId(vendorRequest.getTransferId().toString())
                .vendorPayoutAmount(vendorRequest.getAmount())
                .vendorTransactionTime(vendorRequest.getTs())
                .promoType(PromoType.FREE_ROUND)
                .build();
    }
}
